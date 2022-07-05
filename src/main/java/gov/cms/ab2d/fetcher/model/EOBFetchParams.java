package gov.cms.ab2d.fetcher.model;

import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
public class EOBFetchParams {
    JobFetchPayload jobFetchPayload;
    PatientCoverage patientCoverage;

    public String getJobId() {
        return jobFetchPayload.getJobId();
    }

    public String getOrganization() {
        return jobFetchPayload.getOrganization();
    }

    public boolean isSkipBillablePeriodCheck() {
        return jobFetchPayload.isSkipBillablePeriodCheck();
    }

    public FhirVersion getVersion () {
        return jobFetchPayload.getVersion();
    }

    public OffsetDateTime getSince() {
        return jobFetchPayload.getSince();
    }

    public long getBeneId() {
        return patientCoverage.getBeneId();
    }

    public String getMBI() {
        return patientCoverage.getCurrentMBI();
    }

    public List<FilterOutByDate.DateRange> getDateRanges() {
        return patientCoverage.getDateRanges();
    }

    public JobFetchPayload getJobFetchPayload() {
        return jobFetchPayload;
    }

    public String[] getHistoricMBIs() {
        return patientCoverage.getHistoricMBIs();
    }
}
