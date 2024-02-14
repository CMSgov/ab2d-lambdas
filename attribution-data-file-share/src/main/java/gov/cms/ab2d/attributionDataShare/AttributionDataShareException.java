package gov.cms.ab2d.attributionDataShare;

public class AttributionDataShareException extends RuntimeException {
    public AttributionDataShareException(String message, Exception ex) {
        super(message, ex);
    }
}
