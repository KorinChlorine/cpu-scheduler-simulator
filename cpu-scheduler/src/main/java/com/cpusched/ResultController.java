package com.cpusched;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ResultController implements Initializable {

    // ── Shared static result state ─────────────────────────────────
    private static SchedulingResult        singleResult;
    private static List<SchedulingResult>  compareResults;
    private static boolean                 isCompareMode;

    public static void setSingleResult(SchedulingResult r)        { singleResult = r;  compareResults = null; isCompareMode = false; }
    public static void setCompareResults(List<SchedulingResult> r) { compareResults = r; singleResult = null;  isCompareMode = true;  }

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label   algorithmLabel;
    @FXML private Label   formulaLegend;
    @FXML private StackPane rootPane;

    // Single-mode nodes
    @FXML private HBox    singleModeTop;
    @FXML private HBox    singleModeStats;
    @FXML private VBox    singleModeTable;
    @FXML private VBox    singleModeFormula;
    @FXML private VBox    ganttContainer;
    @FXML private TableView<Process> metricsTable;
    @FXML private TableColumn<Process, String>  mColId;
    @FXML private TableColumn<Process, Integer> mColAT;
    @FXML private TableColumn<Process, Integer> mColBT;
    @FXML private TableColumn<Process, Integer> mColCT;
    @FXML private TableColumn<Process, Integer> mColWT;
    @FXML private TableColumn<Process, Integer> mColTAT;
    @FXML private Label   avgWTLabel;
    @FXML private Label   avgTATLabel;
    @FXML private TextArea formulaArea;

    // Compare-mode panel
    @FXML private VBox    comparePanel;

    private ToastNotification toast;

    // ── Lifecycle ──────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toast = new ToastNotification(rootPane);

        if (isCompareMode && compareResults != null) {
            renderCompareMode();
        } else if (singleResult != null) {
            renderSingleMode();
        }
    }

    // ── Single Mode ────────────────────────────────────────────────

    private void renderSingleMode() {
        algorithmLabel.setText("RESULTS — " + singleResult.getAlgorithmName().toUpperCase());
        formulaLegend.setText(FormulaBuilder.legend());

        comparePanel.setVisible(false);
        comparePanel.setManaged(false);

        showSingleModeSections(true);

        renderGanttSingle(singleResult);
        setupMetricsTable(singleResult.getProcesses());
        displayAverages(singleResult);
        formulaArea.setText(FormulaBuilder.buildBreakdown(singleResult.getProcesses()));
        toast.showSuccess("Simulation complete.");
    }

    // ── Compare Mode ───────────────────────────────────────────────

    private void renderCompareMode() {
        algorithmLabel.setText("COMPARE — ALL ALGORITHMS");
        formulaLegend.setText("Highlights best (lowest) average waiting time and turnaround time.");

        showSingleModeSections(false);

        comparePanel.setVisible(true);
        comparePanel.setManaged(true);
        comparePanel.getChildren().clear();

        double bestWT  = compareResults.stream().mapToDouble(SchedulingResult::getAverageWaitingTime).min().orElse(Double.MAX_VALUE);
        double bestTAT = compareResults.stream().mapToDouble(SchedulingResult::getAverageTurnaroundTime).min().orElse(Double.MAX_VALUE);

        Label sectionLbl = new Label("ALGORITHM COMPARISON");
        sectionLbl.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #8a6040; -fx-padding: 0 0 4 0;");
        comparePanel.getChildren().add(sectionLbl);

        // Two-column bento grid
        HBox row = null;
        for (int i = 0; i < compareResults.size(); i++) {
            if (i % 2 == 0) {
                row = new HBox(14);
                comparePanel.getChildren().add(row);
            }
            VBox card = buildCompareCard(compareResults.get(i), bestWT, bestTAT);
            HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().add(card);
        }

        toast.showSuccess("Compared " + compareResults.size() + " algorithms.");
    }

    private VBox buildCompareCard(SchedulingResult r, double bestWT, double bestTAT) {
        boolean isBestWT  = Math.abs(r.getAverageWaitingTime()    - bestWT)  < 0.001;
        boolean isBestTAT = Math.abs(r.getAverageTurnaroundTime() - bestTAT) < 0.001;

        VBox card = new VBox(8);
        card.getStyleClass().add("bento-cell");
        card.setPadding(new Insets(14));
        if (isBestWT || isBestTAT) {
            card.getStyleClass().add("compare-best");
        }

        // Algorithm name
        Label algoLbl = new Label(r.getAlgorithmName().toUpperCase());
        algoLbl.setStyle("-fx-font-family: Consolas; -fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #5c3317;");

        Map<String, String> colorMap = ProcessColors.buildColorMap(r.getGanttChart());
        GanttCanvas miniGantt = new GanttCanvas(r.getGanttChart(), colorMap, 560);

        javafx.scene.control.ScrollPane ganttScroll = new javafx.scene.control.ScrollPane(miniGantt);
        ganttScroll.setFitToHeight(true);
        ganttScroll.setPrefHeight(100);
        ganttScroll.setMinHeight(86);
        ganttScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        ganttScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ganttScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(4, 0, 0, 0));

        Label wtLbl  = makeStatLabel(String.format("Avg WT: %.2f",  r.getAverageWaitingTime()),
                                     isBestWT  ? "#4a7c59" : "#8a6040");
        Label tatLbl = makeStatLabel(String.format("Avg TAT: %.2f", r.getAverageTurnaroundTime()),
                                     isBestTAT ? "#4a7c59" : "#8a6040");

        String badgeText = (isBestWT && isBestTAT) ? "★ BEST OVERALL"
                         : isBestWT                ? "★ Best WT"
                         : isBestTAT               ? "★ Best TAT"
                         : "";
        stats.getChildren().addAll(wtLbl, tatLbl);
        if (!badgeText.isEmpty()) {
            Label badge = makeStatLabel(badgeText, "#c8860a");
            badge.setStyle(badge.getStyle() + " -fx-font-weight: bold;");
            stats.getChildren().add(badge);
        }

        VBox metricsBox = buildMiniMetrics(r.getProcesses());

        card.getChildren().addAll(algoLbl, ganttScroll, stats, metricsBox);
        return card;
    }

    private VBox buildMiniMetrics(List<Process> processes) {
        VBox box = new VBox(2);
        box.setStyle("-fx-background-color: rgba(200,170,130,0.15); -fx-background-radius: 6; -fx-padding: 6;");

        HBox header = miniRow("Process", "AT", "BT", "CT", "WT", "TAT", true);
        box.getChildren().add(header);

        List<Process> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparing(Process::getId));
        for (Process p : sorted) {
            HBox dataRow = miniRow(
                p.getId(),
                String.valueOf(p.getArrivalTime()),
                String.valueOf(p.getBurstTime()),
                String.valueOf(p.getCompletionTime()),
                String.valueOf(p.getWaitingTime()),
                String.valueOf(p.getTurnaroundTime()),
                false
            );
            if (p.getWaitingTime() > 10) {
                dataRow.getChildren().get(4).setStyle("-fx-text-fill: #c0392b; -fx-font-family: Consolas; -fx-font-size: 11;");
            } else {
                dataRow.getChildren().get(4).setStyle("-fx-text-fill: #27673a; -fx-font-family: Consolas; -fx-font-size: 11;");
            }
            box.getChildren().add(dataRow);
        }
        return box;
    }

    private HBox miniRow(String id, String at, String bt, String ct, String wt, String tat, boolean header) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        String baseStyle = header
            ? "-fx-font-family: Consolas; -fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #5c3317;"
            : "-fx-font-family: Consolas; -fx-font-size: 11; -fx-text-fill: #7a4a20;";
        double[] widths = {55, 38, 38, 52, 38, 52};
        String[] vals   = {id, at, bt, ct, wt, tat};
        for (int i = 0; i < vals.length; i++) {
            Label l = new Label(vals[i]);
            l.setStyle(baseStyle);
            l.setPrefWidth(widths[i]);
            l.setMinWidth(widths[i]);
            row.getChildren().add(l);
        }
        return row;
    }

    private Label makeStatLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: Consolas; -fx-font-size: 12; -fx-text-fill: " + color + ";");
        return l;
    }

    // ── Visibility helpers ─────────────────────────────────────────

    private void showSingleModeSections(boolean show) {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                singleModeTop, singleModeStats, singleModeTable, singleModeFormula}) {
            n.setVisible(show);
            n.setManaged(show);
        }
    }

    // ── Gantt Chart (single mode) ──────────────────────────────────

    private void renderGanttSingle(SchedulingResult r) {
        ganttContainer.getChildren().clear();
        Map<String, String> colorMap = ProcessColors.buildColorMap(r.getGanttChart());
        ganttContainer.getChildren().add(new GanttCanvas(r.getGanttChart(), colorMap));
    }

    // ── Metrics Table ──────────────────────────────────────────────

    private void setupMetricsTable(List<Process> processes) {
        mColId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getId()));
        mColAT.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getArrivalTime()));
        mColBT.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBurstTime()));
        mColCT.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCompletionTime()));
        mColWT.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getWaitingTime()));
        mColTAT.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getTurnaroundTime()));

        mColWT.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle(item > 10
                    ? "-fx-text-fill: #c0392b; -fx-font-family: Consolas;"
                    : "-fx-text-fill: #27673a; -fx-font-family: Consolas;");
            }
        });

        List<Process> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparing(Process::getId));
        metricsTable.setItems(FXCollections.observableArrayList(sorted));
    }

    // ── Averages ───────────────────────────────────────────────────

    private void displayAverages(SchedulingResult r) {
        avgWTLabel.setText(String.format("%.2f", r.getAverageWaitingTime()));
        avgTATLabel.setText(String.format("%.2f", r.getAverageTurnaroundTime()));
    }

    // ── Navigation ─────────────────────────────────────────────────

    @FXML private void onBack() {
        try { App.setRoot("primary"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}