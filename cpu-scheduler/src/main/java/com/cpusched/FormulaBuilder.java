package com.cpusched;

import java.util.List;

/**
 * Generates human-readable formula derivations for scheduling metrics.
 * Used by the Computation Breakdown panel.
 */
public class FormulaBuilder {

    /** Full per-process breakdown + average calculation. */
    public static String buildBreakdown(List<Process> processes) {
        StringBuilder sb = new StringBuilder();
        int n = processes.size();

        sb.append("─── TURNAROUND TIME  (TAT = CT − AT) ───\n");
        double totalTAT = 0;
        for (Process p : processes) {
            sb.append(String.format("  %-4s  TAT = %d − %d = %d\n",
                p.getId(), p.getCompletionTime(), p.getArrivalTime(), p.getTurnaroundTime()));
            totalTAT += p.getTurnaroundTime();
        }
        sb.append(String.format("\n  Avg TAT = ("));
        sb.append(processes.stream()
            .map(p -> String.valueOf(p.getTurnaroundTime()))
            .reduce((a, b) -> a + " + " + b).orElse("0"));
        sb.append(String.format(") / %d = %.2f\n\n", n, totalTAT / n));

        sb.append("─── WAITING TIME  (WT = TAT − BT) ───\n");
        double totalWT = 0;
        for (Process p : processes) {
            sb.append(String.format("  %-4s  WT  = %d − %d = %d\n",
                p.getId(), p.getTurnaroundTime(), p.getBurstTime(), p.getWaitingTime()));
            totalWT += p.getWaitingTime();
        }
        sb.append(String.format("\n  Avg WT  = ("));
        sb.append(processes.stream()
            .map(p -> String.valueOf(p.getWaitingTime()))
            .reduce((a, b) -> a + " + " + b).orElse("0"));
        sb.append(String.format(") / %d = %.2f\n", n, totalWT / n));

        return sb.toString();
    }

    /** One-line formula legend shown in result header. */
    public static String legend() {
        return "TAT = CT − AT   |   WT = TAT − BT   |   Avg = Σ / n";
    }
}
