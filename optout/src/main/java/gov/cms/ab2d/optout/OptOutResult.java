package gov.cms.ab2d.optout;

public class OptOutResult {

    private final OptOutInformation optOutInformation;
    private final String reasonCode;
    private final String recordStatus;

    public OptOutResult(OptOutInformation optOutInformation, String recordStatus, String reasonCode) {
        this.optOutInformation = optOutInformation;
        this.reasonCode = reasonCode;
        this.recordStatus = recordStatus;
    }

    public OptOutInformation getOptOutInformation() {
        return optOutInformation;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
