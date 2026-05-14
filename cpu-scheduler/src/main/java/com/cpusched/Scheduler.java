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

    public static SchedulingResult fcfs(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        processes.sort(Comparator.comparingInt(Process::getArrivalTime)
                .thenComparing(Process::getId));

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;

        for (Process p : processes) {
            if (currentTime < p.getArrivalTime()) {
                gantt.add(new GanttEntry("IDLE", currentTime, p.getArrivalTime()));
                currentTime = p.getArrivalTime();
            }

            int start = currentTime;
            int end = currentTime + p.getBurstTime();

            p.setCompletionTime(end);
            p.setTurnaroundTime(end - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            gantt.add(new GanttEntry(p.getId(), start, end));
            currentTime = end;
        }

        return new SchedulingResult("FCFS", gantt, processes);
    }

    // =========================================================
    // SHORTEST JOB FIRST (SJF) — Non-Preemptive
    // =========================================================
    public static SchedulingResult sjf(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(processes);

        int currentTime = 0;

        while (!remaining.isEmpty()) {
            List<Process> available = new ArrayList<>();
            for (Process p : remaining) {
                if (p.getArrivalTime() <= currentTime) available.add(p);
            }

            if (available.isEmpty()) {
                int nextArrival = remaining.stream()
                        .mapToInt(Process::getArrivalTime).min().orElse(currentTime + 1);
                gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                currentTime = nextArrival;
                continue;
            }

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
    // SHORTEST REMAINING TIME (SRT) — Preemptive SJF
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
                if (!lastProcessId.equals("IDLE")) {
                    if (!lastProcessId.isEmpty()) gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                    blockStart = currentTime;
                    lastProcessId = "IDLE";
                }
                currentTime++;
                continue;
            }

            if (!selected.getId().equals(lastProcessId)) {
                if (!lastProcessId.isEmpty()) {
                    gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                }
                blockStart = currentTime;
                lastProcessId = selected.getId();
            }

            if (!selected.isStarted()) {
                selected.setStartTime(currentTime);
                selected.setStarted(true);
            }

            selected.setRemainingTime(selected.getRemainingTime() - 1);
            currentTime++;

            if (selected.getRemainingTime() == 0) {
                completedCount++;
                selected.setCompletionTime(currentTime);
                selected.setTurnaroundTime(currentTime - selected.getArrivalTime());
                selected.setWaitingTime(selected.getTurnaroundTime() - selected.getBurstTime());
            }
        }

        if (!lastProcessId.isEmpty()) {
            gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
        }

        return new SchedulingResult("SRT (Preemptive SJF)", gantt, processes);
    }

    // =========================================================
    // ROUND ROBIN (RR)
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
                if (!notArrived.isEmpty()) {
                    int nextArrival = notArrived.get(0).getArrivalTime();
                    gantt.add(new GanttEntry("IDLE", currentTime, nextArrival));
                    currentTime = nextArrival;
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

            if (!current.isStarted()) {
                current.setStartTime(start);
                current.setStarted(true);
            }

            gantt.add(new GanttEntry(current.getId(), start, end));
            current.setRemainingTime(current.getRemainingTime() - execTime);
            currentTime = end;

            it = notArrived.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.getArrivalTime() <= currentTime) {
                    readyQueue.add(p);
                    it.remove();
                }
            }

            if (current.getRemainingTime() == 0) {
                current.setCompletionTime(currentTime);
                current.setTurnaroundTime(currentTime - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                completedCount++;
            } else {
                readyQueue.add(current);
            }
        }

        return new SchedulingResult("Round Robin (Q=" + quantum + ")", gantt, processes);
    }

    // =========================================================
    // PRIORITY SCHEDULING — Non-Preemptive
    // =========================================================
    public static SchedulingResult priorityNonPreemptive(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(processes);

        int currentTime = 0;

        while (!remaining.isEmpty()) {
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
    // PRIORITY SCHEDULING WITH ROUND ROBIN
    // =========================================================
    public static SchedulingResult priorityWithRoundRobin(List<Process> input, int quantum) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();

        processes.sort(Comparator.comparingInt(Process::getPriority)
                .thenComparingInt(Process::getArrivalTime));

        Map<Integer, List<Process>> priorityGroups = new LinkedHashMap<>();
        for (Process p : processes) {
            priorityGroups.computeIfAbsent(p.getPriority(), k -> new ArrayList<>()).add(p);
        }

        int currentTime = 0;

        for (Map.Entry<Integer, List<Process>> entry : priorityGroups.entrySet()) {
            List<Process> group = entry.getValue();
            group.sort(Comparator.comparingInt(Process::getArrivalTime));

            Queue<Process> queue = new LinkedList<>();
            List<Process> pending = new ArrayList<>(group);

            int firstArrival = pending.get(0).getArrivalTime();
            if (currentTime < firstArrival) {
                gantt.add(new GanttEntry("IDLE", currentTime, firstArrival));
                currentTime = firstArrival;
            }

            Iterator<Process> it = pending.iterator();
            while (it.hasNext()) {
                Process p = it.next();
                if (p.getArrivalTime() <= currentTime) {
                    queue.add(p);
                    it.remove();
                }
            }

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

    // =========================================================
    // PRIORITY SCHEDULING — Preemptive
    // =========================================================
    public static SchedulingResult priorityPreemptive(List<Process> input) {
        List<Process> processes = copyAndReset(input);
        List<GanttEntry> gantt = new ArrayList<>();

        int n = processes.size();
        int completedCount = 0;
        int currentTime = 0;

        String lastProcessId = "";
        int blockStart = 0;

        while (completedCount < n) {
            Process selected = null;
            int maxPriority = Integer.MAX_VALUE;

            for (Process p : processes) {
                if (p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0) {
                    if (p.getPriority() < maxPriority) {
                        maxPriority = p.getPriority();
                        selected = p;
                    }
                }
            }

            if (selected == null) {
                if (!lastProcessId.equals("IDLE")) {
                    if (!lastProcessId.isEmpty()) gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                    blockStart = currentTime;
                    lastProcessId = "IDLE";
                }
                currentTime++;
                continue;
            }

            if (!selected.getId().equals(lastProcessId)) {
                if (!lastProcessId.isEmpty()) {
                    gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
                }
                blockStart = currentTime;
                lastProcessId = selected.getId();
            }

            if (!selected.isStarted()) {
                selected.setStartTime(currentTime);
                selected.setStarted(true);
            }

            selected.setRemainingTime(selected.getRemainingTime() - 1);
            currentTime++;

            if (selected.getRemainingTime() == 0) {
                completedCount++;
                selected.setCompletionTime(currentTime);
                selected.setTurnaroundTime(currentTime - selected.getArrivalTime());
                selected.setWaitingTime(selected.getTurnaroundTime() - selected.getBurstTime());
            }
        }

        if (!lastProcessId.isEmpty()) {
            gantt.add(new GanttEntry(lastProcessId, blockStart, currentTime));
        }

        return new SchedulingResult("Priority (Preemptive, lower = higher priority)", gantt, processes);
    }
}