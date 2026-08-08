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
    // Constructors
    public ShameEntry() {}

    public ShameEntry(String loggedAt, String location, String triggerMood, String notes) {
        this.loggedAt = loggedAt;
        this.location = location;
        this.triggerMood = triggerMood;
        this.notes = notes;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getTriggerMood() { return triggerMood; }
    public void setTriggerMood(String triggerMood) { this.triggerMood = triggerMood; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    public boolean isWeekend() { return isWeekend; }
    public void setIsWeekend(boolean weekend) { isWeekend = weekend; }
}
