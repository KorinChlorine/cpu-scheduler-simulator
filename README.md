# CPU Scheduling Simulator

A desktop application for visualizing and comparing CPU scheduling algorithms. Enter your processes, pick an algorithm, and instantly see the Gantt chart, turnaround times, waiting times, and more.

---

## Algorithms Supported

| # | Algorithm | Preemptive | Needs Priority | Needs Quantum |
|---|-----------|:----------:|:--------------:|:-------------:|
| 1 | **FCFS** — First-Come, First-Served | No | No | No |
| 2 | **SJF** — Shortest Job First | No | No | No |
| 3 | **SRT** — Shortest Remaining Time | Yes | No | No |
| 4 | **Round Robin** | Yes | No | Yes |
| 5 | **Priority Scheduling** | No | Yes | No |
| 6 | **Priority Scheduling (Preemptive)** | Yes | Yes | No |
| 7 | **Priority + Round Robin** | Yes | Yes | Yes |
| 8 | **Compare All** — runs every algorithm side-by-side | — | — | — |

---

## Requirements

**Java 21 with JavaFX** — the easiest option is **Liberica JDK Full**, which bundles JavaFX out of the box.

> Standard Oracle JDK or OpenJDK will **not** work — they don't include JavaFX.

---

## Installation

### Step 1 — Install Liberica JDK Full

1. Go to [https://bell-sw.com/pages/downloads/#jdk-21-lts](https://bell-sw.com/pages/downloads/#jdk-21-lts)
2. Select:
   - **Version:** 21 LTS
   - **OS:** Windows
   - **Architecture:** amd64
   - **Package:** Full JDK
   - **Format:** `.msi`
3. Download and run the installer. Accept all defaults.

### Step 2 — Download the App

Download the latest release ZIP and extract it anywhere (e.g. your Desktop).

The extracted folder should contain:

```
cpu-scheduler.exe
cpu-scheduler.jar
README.txt
```

### Step 3 — Run

Double-click **`cpu-scheduler.exe`**.

> The `.exe` and `.jar` must stay in the same folder.

---

## Usage

1. **Set process count** using the spinner at the top.
2. **Click Generate Table** to create input rows.
3. **Fill in** each process's Arrival Time, Burst Time, and Priority (if needed).
4. **Select an algorithm** from the dropdown.
5. **Set Time Quantum** if using Round Robin or Priority + RR.
6. **Click Run Simulation** to view results.

On the results screen you'll see:
- Gantt chart with color-coded processes
- Per-process completion time, turnaround time, and waiting time
- Average turnaround and average waiting time
- Side-by-side comparison table (Compare All mode)

Click **Back** to return and adjust inputs.

---

## Troubleshooting

**Nothing happens when I double-click the exe**
- Make sure `cpu-scheduler.jar` is in the same folder as `cpu-scheduler.exe`.
- Make sure Liberica JDK Full is installed (not standard Java).

**"JavaFX not found" error**
- You have a standard JDK installed, not the Full edition.
- Uninstall your current Java, then install [Liberica JDK Full 21](https://bell-sw.com/pages/downloads/#jdk-21-lts).

**"Java not found" error**
- Install [Liberica JDK Full 21](https://bell-sw.com/pages/downloads/#jdk-21-lts) and try again.

---

## Building from Source

Requirements: JDK 17+, Maven 3.8+, JavaFX 17+

```bash
git clone <repo-url>
cd cpu-scheduler
mvn clean package
```

Run with:

```bash
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -m com.cpusched/com.cpusched.App
```
