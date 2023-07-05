package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OptOutInformation {

    private final int mbi;

    private final Timestamp effectiveDate;

    private final boolean optOutFlag;

    public OptOutInformation(int mbi, Timestamp effectiveDate, boolean optOutFlag) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
        this.effectiveDate = effectiveDate;
    }

    public OptOutInformation(String information, LambdaLogger logger) throws IllegalArgumentException {
        try {
            this.mbi = Integer.parseInt(information.substring(0, 11).trim());
            this.optOutFlag = (information.charAt(368) == 'N');
            this.effectiveDate = convertToOffsetDateTime(information.substring(354, 362));
        } catch (NumberFormatException | StringIndexOutOfBoundsException | ParseException ex) {
            logger.log("Lambda can not parse the string: " + information);
            throw new IllegalArgumentException();
        }
    }

    public int getMbi() {
        return mbi;
    }

    public Timestamp getEffectiveDate() {
        return effectiveDate;
    }

    public boolean isOptOut() {
        return optOutFlag;
    }

    private Timestamp convertToOffsetDateTime(String dateString) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date parsedDate = dateFormat.parse(dateString);
        return new Timestamp(parsedDate.getTime());
    }

}
