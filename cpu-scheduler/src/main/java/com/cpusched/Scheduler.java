package com.cpusched;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// Implements all CPU Scheduling Algorithms
public class Scheduler {

    // Deep-copy a list of processes and reset their state
    private static List<Process> copyAndReset(List<Process> original) {
        List<Process> copies = new ArrayList<>();
        for (Process p : original) {
            Process copy = new Process(p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getPriority());
            copies.add(copy);
        }
        return copies;
    }

    // 1. FIRST-COME, FIRST-SERVED (FCFS) — Non-Preemptive
    public static SchedulingResult fcfs(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        // Sort by arrival time; ties broken by process ID order
        processes.sort(Comparator.comparingInt(Process::getArrivalTime)
                .thenComparing(Process::getId));

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;

        for (Process p : processes) {
            // If CPU is idle (no process yet arrived), fast-forward time
            if (currentTime < p.getArrivalTime()) {
                gantt.add(new GanttEntry("IDLE", currentTime, p.getArrivalTime()));
                currentTime = p.getArrivalTime();
            }

            int start = currentTime;
            int end = currentTime + p.getBurstTime();

            // Compute metrics
            p.setCompletionTime(end);
            p.setTurnaroundTime(end - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            gantt.add(new GanttEntry(p.getId(), start, end));
            currentTime = end;
        }

        return new SchedulingResult("FCFS", gantt, processes);
    }

    // =========================================================
    // 2. SHORTEST JOB FIRST (SJF) — Non-Preemptive
    // Among all arrived processes, the one with the shortest burst
    // time is selected next. Optimal average WT but may starve long processes.
    // =========================================================
    public static SchedulingResult sjf(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(processes);

        int currentTime = 0;

        while (!remaining.isEmpty()) {
            // Get all processes that have arrived by currentTime
            List<Process> available = new ArrayList<>();
            for (Process p : remaining) {
                if (p.getArrivalTime() <= currentTime) available.add(p);
            }

            if (available.isEmpty()) {
                // No process available — CPU is idle; jump to next arrival
                int nextArrival = remaining.stream()
                        .mapToInt(Process::getArrivalTime).min().orElse(currentTime + 1);
                gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                currentTime = nextArrival;
                continue;
            }

            // Choose process with shortest burst time (ties: by arrival, then ID)
            available.sort(Comparator.comparingInt(Process::getBurstTime)
                    .thenComparingInt(Process::getArrivalTime)
                    .thenComparing(Process::getId));
            Process selected = available.get(0);
            remaining.remove(selected);

            int start = currentTime;
            int end = currentTime + selected.getBurstTime();

            selected.setCompletionTime(end);
            selected.setTurnaroundTime(end - selected.getArrivalTime());
            selected.setWaitingTime(selected.getTurnaroundTime() - selected.getBurstTime());

            gantt.add(new GanttEntry(selected.getId(), start, end));
            currentTime = end;
            completed.add(selected);
        }

        return new SchedulingResult("SJF (Non-Preemptive)", gantt, completed);
    }

    // =========================================================
    // 3. SHORTEST REMAINING TIME (SRT) — Preemptive SJF
    // At each time unit, the process with the least remaining burst
    // time runs. If a new arrival has shorter remaining time, it preempts.
    // =========================================================
    public static SchedulingResult srt(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();

        int n = processes.size();
        int totalBurst = processes.stream().mapToInt(Process::getBurstTime).sum();
        int maxTime = processes.stream().mapToInt(p -> p.getArrivalTime() + p.getBurstTime()).max().orElse(0);
        int completedCount = 0;
        int currentTime = 0;

        String lastProcessId = "";
        int blockStart = 0;

        while (completedCount < n) {
            // Get available (arrived, not finished) processes
            Process selected = null;
            int minRemaining = Integer.MAX_VALUE;

            for (Process p : processes) {
                if (p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0) {
                    if (p.getRemainingTime() < minRemaining) {
                        minRemaining = p.getRemainingTime();
                        selected = p;
                    }
                }
            }

            if (selected == null) {
                // CPU idle
                if (!lastProcessId.equals("IDLE")) {
                    if (!lastProcessId.isEmpty()) gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                    blockStart = currentTime;
                    lastProcessId = "IDLE";
                }
                currentTime++;
                continue;
            }

            // If the running process changed, close the previous Gantt block
            if (!selected.getId().equals(lastProcessId)) {
                if (!lastProcessId.isEmpty()) {
                    gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                }
                blockStart = currentTime;
                lastProcessId = selected.getId();
            }

            // Record first start time for this process
            if (!selected.isStarted()) {
                selected.setStartTime(currentTime);
                selected.setStarted(true);
            }

            selected.setRemainingTime(selected.getRemainingTime() - 1);
            currentTime++;

            // Check if process just finished
            if (selected.getRemainingTime() == 0) {
                completedCount++;
                selected.setCompletionTime(currentTime);
                selected.setTurnaroundTime(currentTime - selected.getArrivalTime());
                selected.setWaitingTime(selected.getTurnaroundTime() - selected.getBurstTime());
            }
        }

        // Close the last Gantt block
        if (!lastProcessId.isEmpty()) {
            gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
        }

        return new SchedulingResult("SRT (Preemptive SJF)", gantt, processes);
    }

    // =========================================================
    // 4. ROUND ROBIN (RR)
    // Each process gets a fixed time slice (quantum). If it doesn't
    // finish within its quantum, it's preempted and moved to the back
    // of the ready queue. Fair but has higher context switch overhead.
    // =========================================================
    public static SchedulingResult roundRobin(List<Process> input, int quantum) {
        List<Process> processes = copyAndReset(input);
        // Sort initially by arrival time
        processes.sort(Comparator.comparingInt(Process::getArrivalTime).thenComparing(Process::getId));

        List<GanttEntry> gantt = new ArrayList<>();
        Queue<Process> readyQueue = new LinkedList<>();
        List<Process> notArrived = new ArrayList<>(processes);
        int currentTime = 0;
        int completedCount = 0;
        int n = processes.size();

        // Enqueue processes that arrive at time 0
        Iterator<Process> it = notArrived.iterator();
        while (it.hasNext()) {
            Process p = it.next();
            if (p.getArrivalTime() <= currentTime) {
                readyQueue.add(p);
                it.remove();
            }
        }

        while (completedCount < n) {
            if (readyQueue.isEmpty()) {
                // CPU idle — jump to next arrival
                if (!notArrived.isEmpty()) {
                    int nextArrival = notArrived.get(0).getArrivalTime();
                    gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                    currentTime = nextArrival;
                    // Enqueue all processes that have now arrived
                    it = notArrived.iterator();
                    while (it.hasNext()) {
                        Process p = it.next();
                        if (p.getArrivalTime() <= currentTime) {
                            readyQueue.add(p);
                            it.remove();
                        }
                    }
                }
                continue;
            }

            Process current = readyQueue.poll();
            int execTime = Math.min(quantum, current.getRemainingTime());
            int start = currentTime;
            int end = currentTime + execTime;

            // Record first CPU time
            if (!current.isStarted()) {
                current.setStartTime(start);
                current.setStarted(true);
            }

            gantt.add(new GanttEntry(current.getId(), start, end));
            current.setRemainingTime(current.getRemainingTime() - execTime);
            currentTime = end;

            // Enqueue newly arrived processes BEFORE re-queuing the current process
            it = notArrived.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.getArrivalTime() <= currentTime) {
                    readyQueue.add(p);
                    it.remove();
                }
            }

            if (current.getRemainingTime() == 0) {
                // Process finished
                current.setCompletionTime(currentTime);
                current.setTurnaroundTime(currentTime - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                completedCount++;
            } else {
                // Not finished — re-add to back of queue
                readyQueue.add(current);
            }
        }

        return new SchedulingResult("Round Robin (Q=" + quantum + ")", gantt, processes);
    }

    // =========================================================
    // 5. PRIORITY SCHEDULING — Non-Preemptive
    // Among arrived processes, the one with the highest priority
    // (LOWER numeric value = higher priority) is selected next.
    // May cause starvation of low-priority processes.
    // =========================================================
    public static SchedulingResult priorityNonPreemptive(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(processes);

        int currentTime = 0;

        while (!remaining.isEmpty()) {
            // Get all processes that have arrived
            List<Process> available = new ArrayList<>();
            for (Process p : remaining) {
                if (p.getArrivalTime() <= currentTime) available.add(p);
            }

            if (available.isEmpty()) {
                int nextArrival = remaining.stream().mapToInt(Process::getArrivalTime).min().orElse(currentTime + 1);
                gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                currentTime = nextArrival;
                continue;
            }

            // Select process with lowest priority number (= highest priority)
            available.sort(Comparator.comparingInt(Process::getPriority)
                    .thenComparingInt(Process::getArrivalTime)
                    .thenComparing(Process::getId));
            Process selected = available.get(0);
            remaining.remove(selected);

            int start = currentTime;
            int end = currentTime + selected.getBurstTime();

            selected.setCompletionTime(end);
            selected.setTurnaroundTime(end - selected.getArrivalTime());
            selected.setWaitingTime(selected.getTurnaroundTime() - selected.getBurstTime());

            gantt.add(new GanttEntry(selected.getId(), start, end));
            currentTime = end;
            completed.add(selected);
        }

        return new SchedulingResult("Priority (Non-Preemptive, lower = higher priority)", gantt, completed);
    }

    // =========================================================
    // 6. PRIORITY SCHEDULING WITH ROUND ROBIN
    // Processes with the same priority are scheduled using Round Robin.
    // Higher-priority groups are fully served before lower-priority groups.
    // Lower numeric priority value = higher priority.
    // =========================================================
    public static SchedulingResult priorityWithRoundRobin(List<Process> input, int quantum) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();

        // Sort all processes by priority (lower = higher), then arrival
        processes.sort(Comparator.comparingInt(Process::getPriority)
                .thenComparingInt(Process::getArrivalTime));

        // Group processes by priority level
        Map<Integer, List<Process>> priorityGroups = new LinkedHashMap<>();
        for (Process p : processes) {
            priorityGroups.computeIfAbsent(p.getPriority(), k -> new ArrayList<>()).add(p);
        }

        int currentTime = 0;

        // Process each priority group using Round Robin
        for (Map.Entry<Integer, List<Process>> entry : priorityGroups.entrySet()) {
            List<Process> group = entry.getValue();
            // Sort group by arrival time
            group.sort(Comparator.comparingInt(Process::getArrivalTime));

            Queue<Process> queue = new LinkedList<>();
            List<Process> pending = new ArrayList<>(group);

            // Advance time to first arrival in this group if needed
            int firstArrival = pending.get(0).getArrivalTime();
            if (currentTime < firstArrival) {
                gantt.add(new GanttEntry("IDLE", currentTime, firstArrival));
                currentTime = firstArrival;
            }

            // Initially enqueue arrived processes in this group
            Iterator<Process> it = pending.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.getArrivalTime() <= currentTime) {
                    queue.add(p);
                    it.remove();
                }
            }

            // Run Round Robin within this priority group
            while (!queue.isEmpty() || !pending.isEmpty()) {
                if (queue.isEmpty()) {
                    int nextArrival = pending.get(0).getArrivalTime();
                    gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                    currentTime = nextArrival;
                    it = pending.iterator();
                    while (it.hasNext()) {
                        Process p = it.next();
                        if (p.getArrivalTime() <= currentTime) {
                            queue.add(p);
                            it.remove();
                        }
                    }
                    continue;
                }

                Process current = queue.poll();
                int execTime = Math.min(quantum, current.getRemainingTime());
                int start = currentTime;
                int end = currentTime + execTime;

                if (!current.isStarted()) {
                    current.setStartTime(start);
                    current.setStarted(true);
                }

                gantt.add(new GanttEntry(current.getId(), start, end));
                current.setRemainingTime(current.getRemainingTime() - execTime);
                currentTime = end;

                // Enqueue newly arrived processes in this group
                it = pending.iterator();
                while (it.hasNext()) {
                    Process p = it.next();
                    if (p.getArrivalTime() <= currentTime) {
                        queue.add(p);
                        it.remove();
                    }
                }

                if (current.getRemainingTime() == 0) {
                    current.setCompletionTime(currentTime);
                    current.setTurnaroundTime(currentTime - current.getArrivalTime());
                    current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                } else {
                    queue.add(current);
                }
            }
        }

        return new SchedulingResult("Priority + Round Robin (Q=" + quantum + ")", gantt, processes);
    }
}