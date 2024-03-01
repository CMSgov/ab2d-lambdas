package gov.cms.ab2d.optout;

import java.sql.Timestamp;

public class OptOutInformation {
    private final String text;
    private String mbi;
    private boolean optOutFlag;
    private final long lineNumber;

    public OptOutInformation(String mbi, boolean optOutFlag, long lineNumber, String text) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
        this.lineNumber = lineNumber;
        this.text = text;
    }

    public OptOutInformation(long lineNumber, String text) {
        this.lineNumber = lineNumber;
        this.text = text;
    }

    public String getMbi() {
        return mbi;
    }

    public boolean isOptOut() {
        return optOutFlag;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public String getText() {
        return text;
    }
}
