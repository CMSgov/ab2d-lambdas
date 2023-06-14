package gov.cms.ab2d.optout;

public class OptOutException extends RuntimeException {
    public OptOutException(Exception exception) {
        super(exception);
    }
}
