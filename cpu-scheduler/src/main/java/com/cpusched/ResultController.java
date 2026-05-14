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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ResultController implements Initializable {

    // ── Shared static result state ─────────────────────────────────
    private static SchedulingResult        singleResult;
    private static List<SchedulingResult>  compareResults;
    private static boolean                 isCompareMode;

    public static void setSingleResult(SchedulingResult r)       { singleResult = r;  compareResults = null; isCompareMode = false; }
    public static void setCompareResults(List<SchedulingResult> r){ compareResults = r; singleResult = null;  isCompareMode = true;  }

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Label       algorithmLabel;
    @FXML private VBox        ganttContainer;    // holds GanttCanvas (single) or multiple
    @FXML private TableView<Process> metricsTable;
    @FXML private TableColumn<Process, String>  mColId;
    @FXML private TableColumn<Process, Integer> mColAT;
    @FXML private TableColumn<Process, Integer> mColBT;
    @FXML private TableColumn<Process, Integer> mColCT;
    @FXML private TableColumn<Process, Integer> mColWT;
    @FXML private TableColumn<Process, Integer> mColTAT;
    @FXML private Label       avgWTLabel;
    @FXML private Label       avgTATLabel;
    @FXML private TextArea    formulaArea;       // computation breakdown
    @FXML private VBox        comparePanel;      // shown only in compare mode
    @FXML private StackPane   rootPane;
    @FXML private Label       formulaLegend;

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

        renderGanttSingle(singleResult);
        setupMetricsTable(singleResult.getProcesses());
        displayAverages(singleResult);
        formulaArea.setText(FormulaBuilder.buildBreakdown(singleResult.getProcesses()));
        toast.showSuccess("Simulation complete.");
    }

    // ── Compare Mode ───────────────────────────────────────────────

    private void renderCompareMode() {
        algorithmLabel.setText("COMPARE — ALL ALGORITHMS");
        formulaLegend.setText(FormulaBuilder.legend());

        // Hide single-run gantt & table; show compare panel
        ganttContainer.setVisible(false);
        ganttContainer.setManaged(false);
        metricsTable.setVisible(false);
        metricsTable.setManaged(false);
        avgWTLabel.setText("—");
        avgTATLabel.setText("—");
        formulaArea.setText("Run a single algorithm to see step-by-step formula breakdown.");

        comparePanel.setVisible(true);
        comparePanel.setManaged(true);
        comparePanel.getChildren().clear();

        // Determine best (lowest avg WT and avg TAT)
        double bestWT  = compareResults.stream().mapToDouble(SchedulingResult::getAverageWaitingTime).min().orElse(Double.MAX_VALUE);
        double bestTAT = compareResults.stream().mapToDouble(SchedulingResult::getAverageTurnaroundTime).min().orElse(Double.MAX_VALUE);

        for (SchedulingResult r : compareResults) {
            comparePanel.getChildren().add(buildCompareCard(r, bestWT, bestTAT));
        }
        toast.showSuccess("Compared " + compareResults.size() + " algorithms.");
    }

    private VBox buildCompareCard(SchedulingResult r, double bestWT, double bestTAT) {
        boolean isBestWT  = Math.abs(r.getAverageWaitingTime()      - bestWT)  < 0.001;
        boolean isBestTAT = Math.abs(r.getAverageTurnaroundTime()   - bestTAT) < 0.001;

        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(10));
        if (isBestWT || isBestTAT) {
            card.setStyle(card.getStyle() + " -fx-border-color: #c8860a; -fx-border-width: 2;");
        }

        // Algorithm name
        Label algoLbl = new Label(r.getAlgorithmName().toUpperCase());
        algoLbl.setStyle("-fx-font-family: Consolas; -fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #e8c87a;");

        // Mini Gantt
        Map<String, String> colorMap = ProcessColors.buildColorMap(r.getGanttChart());
        GanttCanvas miniGantt = new GanttCanvas(r.getGanttChart(), colorMap);
        miniGantt.setScaleX(0.9);
        miniGantt.setScaleY(0.9);

        // Stats row
        HBox stats = new HBox(24);
        stats.setAlignment(Pos.CENTER_LEFT);

        Label wtLbl  = makeStatLabel("Avg WT: "  + String.format("%.2f", r.getAverageWaitingTime()),
                                     isBestWT  ? "#4ecca3" : "#a0856a");
        Label tatLbl = makeStatLabel("Avg TAT: " + String.format("%.2f", r.getAverageTurnaroundTime()),
                                     isBestTAT ? "#4ecca3" : "#a0856a");

        Label badge = isBestWT && isBestTAT ? makeStatLabel("★ BEST OVERALL", "#c8860a")
                    : isBestWT              ? makeStatLabel("★ Best WT",       "#4ecca3")
                    : isBestTAT             ? makeStatLabel("★ Best TAT",      "#4ecca3")
                    : new Label();

        stats.getChildren().addAll(wtLbl, tatLbl, badge);
        card.getChildren().addAll(algoLbl, miniGantt, stats);
        return card;
    }

    private Label makeStatLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: Consolas; -fx-font-size: 11; -fx-text-fill: " + color + ";");
        return l;
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

        // Color-code WT column
        mColWT.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle(item > 10
                    ? "-fx-text-fill: #e94560; -fx-font-family: Consolas;"
                    : "-fx-text-fill: #4ecca3; -fx-font-family: Consolas;");
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
