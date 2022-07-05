package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.parser.IParser;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.aggregator.ClaimsStream;
import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.events.BeneficiarySearchEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.fetcher.model.EOBFetchParams;
import gov.cms.ab2d.fetcher.model.JobFetchPayload;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl {

    private final BFDClient bfdClient;
    
    private final EventClient eventClient;

    @Value("${efs.mount}")
    private final String efsMount;

    @Value("${aggregator.directory.streaming:streaming}")
    private final String streamingDir;

    @Value("${aggregator.directory.finished:finished}")
    private final String finishedDir;

    public void writeOutData(JobFetchPayload jobFetchPayload, ProgressTrackerUpdate update) throws IOException {
        File file = null;
        try (ClaimsStream stream = buildClaimStream(jobFetchPayload.getJobId(), DATA)) {
            file = stream.getFile();
            eventClient.send(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    stream.getFile(), FileEvent.FileStatus.OPEN));
            for (EOBFetchParams fetchParams : jobFetchPayload.buildParams()) {
                List<IBaseResource> eobs = getEobBundleResources(fetchParams);
                writeOutResource(jobFetchPayload, fetchParams.getVersion(), update, eobs, stream);
                update.incPatientProcessCount();
            }
        } finally {
            eventClient.send(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    file, FileEvent.FileStatus.CLOSE));
        }
    }

    void writeOutErrors(String anyErrors, JobFetchPayload jobFetchPayload) {
        File errorFile = null;
        try (ClaimsStream stream = buildClaimStream(jobFetchPayload.getJobId(), ERROR)) {
            errorFile = stream.getFile();
            eventClient.send(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    stream.getFile(), FileEvent.FileStatus.OPEN));
            stream.write(anyErrors);
        } catch (IOException e) {
            log.error("Cannot log error to error file");
        } finally {
            eventClient.send(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    errorFile, FileEvent.FileStatus.CLOSE));
        }
    }

    private ClaimsStream buildClaimStream(String jobId, FileOutputType outputType) throws IOException {
        return new ClaimsStream(jobId, efsMount, outputType,
                streamingDir, finishedDir, (int) FileUtils.ONE_MB);
    }

    @Trace(metricName = "EOBWriteToFile", dispatcher = true)
    private void writeOutResource(JobFetchPayload jobFetchPayload, FhirVersion version, ProgressTrackerUpdate update,
                                  List<IBaseResource> eobs, ClaimsStream stream) {
        IParser parser = version.getJsonParser().setPrettyPrint(false);
        if (eobs == null) {
            log.debug("ignoring empty results because pulling eobs failed");
            return;
        }

        if (eobs.isEmpty()) {
            return;
        }
        int eobsWritten = 0;
        int eobsError = 0;

        update.incPatientsWithEobsCount();
        update.addEobFetchedCount(eobs.size());

        StringBuilder errorPayload = new StringBuilder();
        for (IBaseResource resource : eobs) {
            try {
                stream.write(parser.encodeResourceToString(resource) + System.lineSeparator());
                eobsWritten++;
            } catch (Exception ex) {
                log.warn("Encountered exception while processing job resources: {}", ex.getClass());
                String errMsg = ExceptionUtils.getRootCauseMessage(ex);
                IBaseResource operationOutcome = version.getErrorOutcome(errMsg);
                errorPayload.append(parser.encodeResourceToString(operationOutcome)).append(System.lineSeparator());
                eobsError++;
            }
        }

        update.addEobProcessedCount(eobsWritten);

        if (eobsError != 0) {
            writeOutErrors(errorPayload.toString(), jobFetchPayload);
        }
    }

    /**
     * Begin requesting claims from BFD using the provided, page through
     * the resulting claims until none are left, filter claims not meeting requirements, and filter out fields
     * in claims that AB2D cannot provide.
     *
     * @return list of matching claims after filtering claims not meeting requirements and stripping fields that AB2D
     * cannot provide
     */
    @Trace(metricName = "EOBRequest", dispatcher = true)
    private List<IBaseResource> getEobBundleResources(EOBFetchParams fetchParams) {

        OffsetDateTime requestStartTime = OffsetDateTime.now();

        // Aggregate claims into a single list
        PatientClaimsCollector collector = new PatientClaimsCollector();

        IBaseBundle eobBundle;

        try {

            // Set header for requests so BFD knows where this request originated from
            BFDClient.BFD_BULK_JOB_ID.set(fetchParams.getJobId());

            // Make first request and begin looping over remaining pages
            eobBundle = bfdClient.requestEOBFromServer(fetchParams.getVersion(),
                    fetchParams.getBeneId(), fetchParams.getSince());
            collector.filterAndAddEntries(eobBundle, fetchParams);

            while (BundleUtils.getNextLink(eobBundle) != null) {
                eobBundle = bfdClient.requestNextBundleFromServer(fetchParams.getVersion(), eobBundle);
                collector.filterAndAddEntries(eobBundle, fetchParams);
            }

            // Log request to Kinesis and NewRelic
            logSuccessful(fetchParams, requestStartTime);
            return collector.getEobs();
        } catch (Exception ex) {
            logError(fetchParams, requestStartTime, ex);
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }
    }

    private void logSuccessful(EOBFetchParams eobFetchParams, OffsetDateTime start) {
        eventClient.send(
                new BeneficiarySearchEvent(eobFetchParams.getOrganization(), eobFetchParams.getJobId(), "",
                        start, OffsetDateTime.now(),
                        eobFetchParams.getBeneId(),
                        "SUCCESS"));
    }

    private void logError(EOBFetchParams eobFetchParams, OffsetDateTime start, Exception ex) {
        eventClient.send(
                new BeneficiarySearchEvent(eobFetchParams.getOrganization(), eobFetchParams.getJobId(), "",
                        start, OffsetDateTime.now(),
                        eobFetchParams.getBeneId(),
                        "ERROR: " + ex.getMessage()));
    }
}
