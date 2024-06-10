package gov.cms.ab2d.optout;

public class OptOutResults {

    private final int totalFromDB;
    private final int optInToday;
    private final int optOutToday;

    public OptOutResults(int totalFromDB, int optInToday, int optOutToday){
        this.totalFromDB = totalFromDB;
        this.optInToday = optInToday;
        this.optOutToday = optOutToday;
    }

    public int getTotalFromDB() {
        return totalFromDB;
    }
    
    public int getOptInToday() {
        return optInToday;
    }

    public int getOptOutToday() {
        return optOutToday;
    }

    public int getTotalToday() {
        return optInToday + optOutToday;
    }

}
