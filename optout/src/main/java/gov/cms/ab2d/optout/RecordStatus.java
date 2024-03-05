package gov.cms.ab2d.optout;


public enum RecordStatus {

    ACCEPTED("Accepted", "00"),
    REJECTED("Rejected", "02");
    public final String status;
    public final String code;
    RecordStatus(String status, String code) {
        this.status = status;
        this.code = code;
    }

}


