package gov.cms.ab2d.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public class MetricAlarm {
    @JsonProperty("AlarmName")
    private String alarmName;
    @JsonProperty("AlarmDescription")
    private String alarmDescription;
    @JsonProperty("AWSAccountId")
    private String awsAccountId;
    @JsonProperty("AlarmConfigurationUpdatedTimestamp")
    private String alarmConfigurationUpdatedTimestamp;
    @JsonProperty("NewStateValue")
    private String newStateValue;
    @JsonProperty("NewStateReason")
    private String newStateReason;
    @JsonProperty("StateChangeTime")
    private String stateChangeTime;
    @JsonProperty("Region")
    private String region;
    @JsonProperty("AlarmArn")
    private String alarmArn;
    @JsonProperty("OldStateValue")
    private String oldStateValue;
    @JsonProperty("OKActions")
    private List<String> okActions;
    @JsonProperty("AlarmActions")
    private List<String> alarmActions;
    @JsonProperty("InsufficientDataActions")
    private List<Object> insufficientDataActions;
    @JsonProperty("Trigger")
    private Trigger trigger;

    public MetricAlarm() {
        //default constructor for Jackson
    }

    @JsonProperty("Namespace")
    public String getNamespace() {
        return Optional.ofNullable(trigger)
                .orElse(new Trigger())
                .getNamespace();
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    public String getAwsAccountId() {
        return awsAccountId;
    }

    public void setAwsAccountId(String awsAccountId) {
        this.awsAccountId = awsAccountId;
    }

    public String getAlarmConfigurationUpdatedTimestamp() {
        return alarmConfigurationUpdatedTimestamp;
    }

    public void setAlarmConfigurationUpdatedTimestamp(String alarmConfigurationUpdatedTimestamp) {
        this.alarmConfigurationUpdatedTimestamp = alarmConfigurationUpdatedTimestamp;
    }

    public String getNewStateValue() {
        return newStateValue;
    }

    public void setNewStateValue(String newStateValue) {
        this.newStateValue = newStateValue;
    }

    public String getNewStateReason() {
        return newStateReason;
    }

    public void setNewStateReason(String newStateReason) {
        this.newStateReason = newStateReason;
    }

    public String getStateChangeTime() {
        return stateChangeTime;
    }

    public void setStateChangeTime(String stateChangeTime) {
        this.stateChangeTime = stateChangeTime;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAlarmArn() {
        return alarmArn;
    }

    public void setAlarmArn(String alarmArn) {
        this.alarmArn = alarmArn;
    }

    public String getOldStateValue() {
        return oldStateValue;
    }

    public void setOldStateValue(String oldStateValue) {
        this.oldStateValue = oldStateValue;
    }

    public List<String> getOkActions() {
        return okActions;
    }

    public void setOkActions(List<String> okActions) {
        this.okActions = okActions;
    }

    public List<String> getAlarmActions() {
        return alarmActions;
    }

    public void setAlarmActions(List<String> alarmActions) {
        this.alarmActions = alarmActions;
    }

    public List<Object> getInsufficientDataActions() {
        return insufficientDataActions;
    }

    public void setInsufficientDataActions(List<Object> insufficientDataActions) {
        this.insufficientDataActions = insufficientDataActions;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }
}
