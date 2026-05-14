package com.cpusched;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// JavaFX-friendly observable wrapper for process input data
public class ProcessRow {
    private final StringProperty id;
    private final IntegerProperty arrivalTime;
    private final IntegerProperty burstTime;
    private final IntegerProperty priority;

    public ProcessRow(String id, int arrivalTime, int burstTime, int priority) {
        this.id = new SimpleStringProperty(id);
        this.arrivalTime = new SimpleIntegerProperty(arrivalTime);
        this.burstTime = new SimpleIntegerProperty(burstTime);
        this.priority = new SimpleIntegerProperty(priority);
    }

    // -- ID --
    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }
    public void setId(String value) { id.set(value); }

    // -- Arrival Time --
    public IntegerProperty arrivalTimeProperty() { return arrivalTime; }
    public int getArrivalTime() { return arrivalTime.get(); }
    public void setArrivalTime(int value) { arrivalTime.set(value); }

    // -- Burst Time --
    public IntegerProperty burstTimeProperty() { return burstTime; }
    public int getBurstTime() { return burstTime.get(); }
    public void setBurstTime(int value) { burstTime.set(value); }

    // -- Priority --
    public IntegerProperty priorityProperty() { return priority; }
    public int getPriority() { return priority.get(); }
    public void setPriority(int value) { priority.set(value); }

    public Process toProcess() {
        return new Process(getId(), getArrivalTime(), getBurstTime(), getPriority());
    }
}