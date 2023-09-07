package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class OptOutInformation {

    private final String mbi;

    private final boolean optOutFlag;

    public OptOutInformation(String mbi, boolean optOutFlag) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
    }

    public OptOutInformation(String information, LambdaLogger logger) throws IllegalArgumentException {
        try {
            this.mbi = information.substring(0, 11).trim();
            this.optOutFlag = (information.charAt(368) == 'Y');
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
            logger.log("Lambda can not parse the string: " + information);
            throw new IllegalArgumentException();
        }
    }

    public String getMbi() {
        return mbi;
    }

    public boolean isOptOut() {
        return optOutFlag;
    }


}
