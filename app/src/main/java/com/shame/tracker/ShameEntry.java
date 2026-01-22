package com.shame.tracker;

import com.google.gson.annotations.SerializedName;

public class ShameEntry {
    @SerializedName("id")
    private String id;

    @SerializedName("logged_at")
    private String loggedAt;

    @SerializedName("location")
    private String location;

    @SerializedName("trigger_mood")
    private String triggerMood;

    @SerializedName("cost")
    private double cost;

    @SerializedName("notes")
    private String notes;

    @SerializedName("day_of_week")
    private Integer dayOfWeek;

    @SerializedName("hour")
    private Integer hour;

    @SerializedName("minute")
    private Integer minute;

    @SerializedName("is_weekend")
    private boolean isWeekend;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getLoggedAt() {
        return loggedAt;
    }

    public String getLocation() {
        return location;
    }

    public String getTriggerMood() {
        return triggerMood;
    }

    public double getCost() {
        return cost;
    }

    public String getNotes() {
        return notes;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public Integer getHour() {
        return hour;
    }

    public Integer getMinute() {
        return minute;
    }

    public boolean isWeekend() {
        return isWeekend;
    }
}
