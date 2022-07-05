package gov.cms.ab2d.fetcher.model;

import gov.cms.ab2d.filter.FilterOutByDate;
import lombok.Getter;

import java.util.List;

@Getter
public class PatientCoverage {
    long beneId;
    String currentMBI;
    String[] historicMBIs;
    private List<FilterOutByDate.DateRange> dateRanges;
}
