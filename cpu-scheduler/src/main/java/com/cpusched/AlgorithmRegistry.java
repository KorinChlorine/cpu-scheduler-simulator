package com.cpusched;

import java.util.Arrays;
import java.util.List;


public class AlgorithmRegistry {

    public enum AlgoId {
        FCFS, SJF, SRT, RR, PRIORITY, PRIORITY_RR, COMPARE_ALL
    }

    public record AlgorithmMeta(
        AlgoId id,
        String displayName,
        boolean needsQuantum,
        boolean needsPriority
    ) {}

    public static final List<AlgorithmMeta> ALL = Arrays.asList(
        new AlgorithmMeta(AlgoId.FCFS,        "1. FCFS — First-Come, First-Served",          false, false),
        new AlgorithmMeta(AlgoId.SJF,         "2. SJF — Shortest Job First (Non-Preemptive)",false, false),
        new AlgorithmMeta(AlgoId.SRT,         "3. SRT — Shortest Remaining Time (Preemptive)",false, false),
        new AlgorithmMeta(AlgoId.RR,          "4. Round Robin",                               true,  false),
        new AlgorithmMeta(AlgoId.PRIORITY,    "5. Priority Scheduling (Non-Preemptive)",      false, true),
        new AlgorithmMeta(AlgoId.PRIORITY_RR, "6. Priority Scheduling + Round Robin",         true,  true),
        new AlgorithmMeta(AlgoId.COMPARE_ALL, "7. Compare All Algorithms",                  true,  true)
    );

    public static AlgorithmMeta get(int index) {
        return ALL.get(index);
    }

    public static List<String> displayNames() {
        return ALL.stream().map(AlgorithmMeta::displayName).toList();
    }

    public static SchedulingResult run(AlgoId id, List<Process> processes, int quantum) {
        return switch (id) {
            case FCFS        -> Scheduler.fcfs(processes);
            case SJF         -> Scheduler.sjf(processes);
            case SRT         -> Scheduler.srt(processes);
            case RR          -> Scheduler.roundRobin(processes, quantum);
            case PRIORITY    -> Scheduler.priorityNonPreemptive(processes);
            case PRIORITY_RR -> Scheduler.priorityWithRoundRobin(processes, quantum);
            case COMPARE_ALL -> throw new UnsupportedOperationException("Use runAll()");
        };
    }

    /** Run all single algorithms and return their results for comparison. */
    public static List<SchedulingResult> runAll(List<Process> processes, int quantum) {
        return ALL.stream()
            .filter(m -> m.id() != AlgoId.COMPARE_ALL)
            .map(m -> run(m.id(), processes, quantum))
            .toList();
    }
}
