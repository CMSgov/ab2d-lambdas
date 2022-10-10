package gov.cms.ab2d.metrics;

public class Dimensions {
    private String value;
    private String name;

    public Dimensions(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public Dimensions() {
        //default constructor for Jackson
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
