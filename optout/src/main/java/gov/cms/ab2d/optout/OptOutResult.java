package gov.cms.ab2d.optout;

public class OptOutResult {

    private final OptOutInformation optOutInformation;
    private final RecordStatus recordStatus;

    public OptOutResult(OptOutInformation optOutInformation, RecordStatus recordStatus) {
        this.optOutInformation = optOutInformation;
        this.recordStatus = recordStatus;
    }

    public OptOutInformation getOptOutInformation() {
        return optOutInformation;
    }

    public RecordStatus getRecordStatus() {
        return recordStatus;
    }

}
