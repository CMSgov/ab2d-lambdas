package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class OptOutInformation {

    private final int mbi;

    private final OffsetDateTime effectiveDate;

    private final boolean optOutFlag;

    private final ZoneId zoneId = ZoneId.of("UTC");

    public OptOutInformation(int mbi, OffsetDateTime effectiveDate, boolean optOutFlag) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
        this.effectiveDate = effectiveDate;
    }

    public OptOutInformation(String information, LambdaLogger logger) throws IllegalArgumentException {
        try {
            this.mbi = Integer.parseInt(information.substring(0, 11).trim());
            this.optOutFlag = (information.charAt(368) == 'N');
            this.effectiveDate = convertToOffsetDateTime(information.substring(354, 362));
        } catch (NumberFormatException | StringIndexOutOfBoundsException | DateTimeParseException ex) {
            logger.log("Lambda can not parse the string: " + information);
            throw new IllegalArgumentException();
        }
    }

    public int getMbi() {
        return mbi;
    }

    public OffsetDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public boolean isOptOut() {
        return optOutFlag;
    }

    private OffsetDateTime convertToOffsetDateTime(String dateString) {
        LocalDateTime date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
        ZoneOffset offset = zoneId.getRules().getOffset(date);
        return OffsetDateTime.of(date, offset);
    }

}
