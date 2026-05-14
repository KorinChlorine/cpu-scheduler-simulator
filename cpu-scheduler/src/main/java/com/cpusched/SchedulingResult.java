package com.cpusched;

import java.util.List;

public class SchedulingResult {
    private List<GanttEntry> ganttChart;
    private List<Process> processes;
    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private String algorithmName;

    public SchedulingResult(String algorithmName, List<GanttEntry> ganttChart, List<Process> processes) {
        this.algorithmName = algorithmName;
        this.ganttChart = ganttChart;
        this.processes = processes;
        computeAverages();
    }

    private void computeAverages() {
        if (processes == null || processes.isEmpty()) return;
        double totalWT = 0, totalTAT = 0;
        for (Process p : processes) {
            totalWT += p.getWaitingTime();
            totalTAT += p.getTurnaroundTime();
        }
        this.averageWaitingTime = totalWT / processes.size();
        this.averageTurnaroundTime = totalTAT / processes.size();
    }

    public List<GanttEntry> getGanttChart() { return ganttChart; }
    public List<Process> getProcesses() { return processes; }
    public double getAverageWaitingTime() { return averageWaitingTime; }
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public String getAlgorithmName() { return algorithmName; }
}