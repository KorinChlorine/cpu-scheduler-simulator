package com.cpusched;

// Represents a single process in the CPU scheduling simulation
public class Process {
    private String id;
    private int arrivalTime;
    private int burstTime;
    private int priority;

    private int waitingTime;
    private int turnaroundTime;
    private int completionTime;

    private int remainingTime;
    private int startTime;
    private boolean started;

    public Process(String id, int arrivalTime, int burstTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
        this.started = false;
    }

    public void reset() {
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.completionTime = 0;
        this.startTime = 0;
        this.started = false;
    }

    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getPriority() { return priority; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }

    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }

    @Override
    public String toString() {
        return id + " [AT=" + arrivalTime + ", BT=" + burstTime + ", PR=" + priority + "]";
    }
}