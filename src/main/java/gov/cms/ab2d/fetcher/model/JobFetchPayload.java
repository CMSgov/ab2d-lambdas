package gov.cms.ab2d.fetcher.model;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;

@Getter
public class JobFetchPayload {

    String jobId;  // collation_id in the request
    String organization;
    boolean skipBillablePeriodCheck;
    FhirVersion version;
    OffsetDateTime since;  // required, caller needs to validate and provide default
    PatientCoverage[] beneBatch;

    public Iterable<EOBFetchParams> buildParams() {
        return () -> new Iterator<EOBFetchParams>() {
            final Iterator<PatientCoverage> coverageIterator = Arrays.stream(beneBatch).iterator();

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
