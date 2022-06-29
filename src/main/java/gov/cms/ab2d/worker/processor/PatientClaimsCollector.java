package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.fetcher.model.EOBFetchParams;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.ExtensionUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.filter.FilterEob;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Collect and filter claims based on AB2D business requirements and allow documenting the results of all actions.
 *
 * Relevant classes influencing filtering and behavior:
 *      - {@link ExplanationOfBenefitTrimmer#getBenefit} strip fields that AB2D should not provide based on {@link gov.cms.ab2d.fhir.FhirVersion}
 *      - {@link EobUtils#isPartD} remove claims that are PartD
 */
@Slf4j
public class PatientClaimsCollector {

    private final List<IBaseResource> eobs;

    public PatientClaimsCollector() {
        this.eobs = new ArrayList<>();
    }

    public List<IBaseResource> getEobs() {
        return eobs;
    }

    /**
     * Filter out EOBs not meeting requirements and add on MBIs to remaining claims
     *
     * This method implements business requirements for AB2D. Do not change this method without consulting
     * multiple people concerning the implications.
     *
     * Filters include:
     *      - filter out if billable period does not match a date range where contracts were enrolled
     *      - filter out fields that AB2D is not allowed to report with claims data
     *      - filter out if eob belongs to Part D
     *      - filter out if eob patient id does not match original request patient id
     *
     * Billable period filters are applied to all contract types except for
     *
     * @param bundle response from BFD containing a list of claims for a specific requested patient
     */
    public void filterAndAddEntries(IBaseBundle bundle, EOBFetchParams patient) {

        // Skip if bundle is missing for some reason
        if (bundle == null) {
            return;
        }


        // Returns null if bundle is null
        List<IBaseBackboneElement> bundleEntries = BundleUtils.getEntries(bundle);
        if (bundleEntries == null) {
            log.error("bundle entries not found for bundle");
            return;
        }

        Date earliest = new Date(patient.getSince().toEpochSecond() * 1000);

        // Perform filtering actions
        BundleUtils.getEobResources(bundleEntries).stream()
                // Filter by date unless contract is an old synthetic data contract, part D or attestation time is null
                // Filter out data
                .filter(resource -> FilterEob.filter(resource, patient.getDateRanges(), earliest, earliest,
                        patient.isSkipBillablePeriodCheck()).isPresent())
                // Filter out unnecessary fields
                .map(ExplanationOfBenefitTrimmer::getBenefit)
                // Make sure patients are the same
                .filter(resource -> matchingPatient(resource, patient.getBeneId()))
                // Make sure update date is after since date
                .filter(eob -> afterSinceDate(eob, patient))
                .forEach(eob -> addEobsToList(eobs, eob, patient));
    }

    private void addEobsToList(List<IBaseResource> eobs, IBaseResource eob, EOBFetchParams patient) {
        addMbiIdsToEobs(eob, patient);
        eobs.add(eob);
    }

    /**
     * We want to make sure that the last updated date is not before the _since
     * date. This should never happen, but it's a sanity check on BFD in case they
     * want to do this to help people who've made missed out on data and ignore
     * the _since date
     *
     * @param resource - the EOB
     * @return true if the lastUpdated date is after the since date
     */
    boolean afterSinceDate(IBaseResource resource, EOBFetchParams eobFetchParams) {
        OffsetDateTime sinceTime = eobFetchParams.getSince();
        if (sinceTime == null) {
            return true;
        }
        Date lastUpdated = resource.getMeta().getLastUpdated();
        if (lastUpdated == null) {
            return false;
        }
        return sinceTime.toInstant().toEpochMilli() < lastUpdated.getTime();
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit  - The benefit to check
     * @return true if this patient is a member of the correct contract
     */
    private boolean matchingPatient(IBaseResource benefit, long requestedPatientId) {

        Long patientId = EobUtils.getPatientId(benefit);
        if (patientId == null || requestedPatientId != patientId) {
            log.error(patientId + " returned in EOB object, but does not match beneficiary id passed to the search");
            return false;
        }
        return true;
    }

    private void addMbiIdsToEobs(IBaseResource eob, EOBFetchParams patient) {
        if (eob == null) {
            return;
        }

        // Add extesions only if beneficiary id is present and known to memberships
        Long benId = EobUtils.getPatientId(eob);
        if (benId != null && patient != null) {

            // Add each mbi to each eob
            if (patient.getMBI() != null) {
                IBase currentMbiExtension = ExtensionUtils.createMbiExtension(patient.getMBI(), true, patient.getVersion());
                ExtensionUtils.addExtension(eob, currentMbiExtension, patient.getVersion());
            }

            for (String mbi : patient.getHistoricMBIs()) {
                IBase mbiExtension = ExtensionUtils.createMbiExtension(mbi, false, patient.getVersion());
                ExtensionUtils.addExtension(eob, mbiExtension, patient.getVersion());
            }
        }
    }
}
