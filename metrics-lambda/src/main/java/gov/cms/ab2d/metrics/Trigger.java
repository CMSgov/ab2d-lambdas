package gov.cms.ab2d.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trigger {
    @JsonProperty("Dimensions")
    private Dimensions[] dimensions;
    @JsonProperty("MetricName")
    private String metricName;
    @JsonProperty("Namespace")
    private String namespace;
    @JsonProperty("StatisticType")
    private String statisticType;
    @JsonProperty("Statistic")
    private String statistic;
    @JsonProperty("Unit")
    private String unit;
    @JsonProperty("Period")
    private int period;
    @JsonProperty("EvaluationPeriods")
    private String evaluationPeriods;
    @JsonProperty("ComparisonOperator")
    private String comparisonOperator;
    @JsonProperty("Threshold")
    private int threshold;
    @JsonProperty("TreatMissingData")
    private String treatMissingData;
    @JsonProperty("EvaluateLowSampleCountPercentile")
    private String evaluateLowSampleCountPercentile;

    public Trigger() {
        //default constructor for Jackson
    }

    public Dimensions[] getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions[] dimensions) {
        this.dimensions = dimensions;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatisticType() {
        return statisticType;
    }

    public void setStatisticType(String statisticType) {
        this.statisticType = statisticType;
    }

    public String getStatistic() {
        return statistic;
    }

    public void setStatistic(String statistic) {
        this.statistic = statistic;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public String getEvaluationPeriods() {
        return evaluationPeriods;
    }

    public void setEvaluationPeriods(String evaluationPeriods) {
        this.evaluationPeriods = evaluationPeriods;
    }

    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getTreatMissingData() {
        return treatMissingData;
    }

    public void setTreatMissingData(String treatMissingData) {
        this.treatMissingData = treatMissingData;
    }

    public String getEvaluateLowSampleCountPercentile() {
        return evaluateLowSampleCountPercentile;
    }

    public void setEvaluateLowSampleCountPercentile(String evaluateLowSampleCountPercentile) {
        this.evaluateLowSampleCountPercentile = evaluateLowSampleCountPercentile;
    }
}
