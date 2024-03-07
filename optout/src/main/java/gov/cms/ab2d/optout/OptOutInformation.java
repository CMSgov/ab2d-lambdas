package gov.cms.ab2d.optout;

public class OptOutInformation {
    private final String mbi;
    private final Boolean optOutFlag;
    public OptOutInformation(String mbi, Boolean optOutFlag) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
    }
    public Boolean getOptOutFlag() {
        return optOutFlag;
    }
    public String getMbi() {
        return mbi;
    }

}