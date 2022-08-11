package gov.cms.ab2d.fetcher.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;

@Getter
@ToString
public class JobFetchPayload {

    String jobId;  // collation_id in the request
    String organization;
    boolean skipBillablePeriodCheck;
    FhirVersion version;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSz")
    OffsetDateTime since;  // required, caller needs to validate and provide default
    PatientCoverage[] beneficiaries;

    public Iterable<EOBFetchParams> buildParams() {
        return () -> new Iterator<>() {
            final Iterator<PatientCoverage> coverageIterator = Arrays.stream(beneficiaries).iterator();

            @Override
            public boolean hasNext() {
                return coverageIterator.hasNext();
            }

            @Override
            public EOBFetchParams next() {
                return new EOBFetchParams(JobFetchPayload.this, coverageIterator.next());
            }
        };
    }
}
