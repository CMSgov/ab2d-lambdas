package gov.cms.ab2d.optout;

public class OptOutMessage {

    private final OptOutInformation optOutInformation;
    private final boolean poisonPill;

    public OptOutMessage(OptOutInformation optOutInformation, boolean poisonPill) {
        this.optOutInformation = optOutInformation;
        this.poisonPill = poisonPill;
    }

    public OptOutInformation getOptOutInformation() {
        return optOutInformation;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}
