package com.cpusched;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.cpusched.AlgorithmRegistry.AlgoId;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class PrimaryController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Spinner<Integer> processCountSpinner;
    @FXML private VBox             processInputContainer;
    @FXML private ComboBox<String> algorithmCombo;
    @FXML private Spinner<Integer> quantumSpinner;
    @FXML private HBox             quantumBox;
    @FXML private Label            statusLabel;
    @FXML private StackPane        rootPane;

    // ── State ──────────────────────────────────────────────────────
    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();
    private ToastNotification toast;

    // Persisted across back-navigation
    static ObservableList<ProcessRow> persistedRows = FXCollections.observableArrayList();
    static int                         lastAlgoIndex = 0;
    static int                         lastQuantum   = 2;

    // ── Lifecycle ──────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toast = new ToastNotification(rootPane);

        if (!persistedRows.isEmpty()) {
            processRows.setAll(persistedRows);
        } else {
            generateDefaultRows(processCountSpinner.getValue());
        }

        setupAlgorithmCombo();
        refreshFormUI();

        algorithmCombo.getSelectionModel().select(lastAlgoIndex);
        quantumSpinner.getValueFactory().setValue(lastQuantum);
        updateQuantumVisibility();
    }

    // ── Form Building ──────────────────────────────────────────────

    private void refreshFormUI() {
        processInputContainer.getChildren().clear();
        processInputContainer.getChildren().add(createHeaderRow());
        for (ProcessRow row : processRows) {
            processInputContainer.getChildren().add(createInputRow(row));
        }
    }

    private HBox createHeaderRow() {
        HBox header = new HBox(10);
        header.setStyle("-fx-padding: 8 5; -fx-background-color: rgba(46,26,14,0.5); -fx-border-radius: 4;");
        String[] labels = {"Process ID", "Arrival Time", "Burst Time", "Priority"};
        double[] widths = {80, 90, 90, 80};
        for (int i = 0; i < labels.length; i++) {
            Label l = new Label(labels[i]);
            l.setPrefWidth(widths[i]);
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: #e8c87a; -fx-font-family: Consolas; -fx-font-size: 11;");
            header.getChildren().add(l);
        }
        return header;
    }

    private HBox createInputRow(ProcessRow row) {
        HBox inputRow = new HBox(10);
        inputRow.setPadding(new Insets(6));
        inputRow.setStyle("-fx-background-color: rgba(46,26,14,0.3); -fx-border-radius: 4;");

        TextField idField       = makeTextField(row.getId(),                        80);
        TextField arrivalField  = makeTextField(String.valueOf(row.getArrivalTime()), 90);
        TextField burstField    = makeTextField(String.valueOf(row.getBurstTime()),   90);
        TextField priorityField = makeTextField(String.valueOf(row.getPriority()),    80);

        idField.textProperty().addListener((obs, o, n) -> row.setId(n));
        arrivalField.textProperty().addListener((obs, o, n) -> parseAndSet(n, row::setArrivalTime));
        burstField.textProperty().addListener((obs, o, n) -> parseAndSet(n, row::setBurstTime));
        priorityField.textProperty().addListener((obs, o, n) -> parseAndSet(n, row::setPriority));

        inputRow.getChildren().addAll(idField, arrivalField, burstField, priorityField);
        return inputRow;
    }

    private TextField makeTextField(String value, double width) {
        TextField f = new TextField(value);
        f.setPrefWidth(width);
        f.getStyleClass().add("form-input");
        return f;
    }

    private void parseAndSet(String text, java.util.function.IntConsumer setter) {
        try { if (!text.isEmpty()) setter.accept(Integer.parseInt(text)); }
        catch (NumberFormatException ignored) {}
    }

    // ── Algorithm Setup ────────────────────────────────────────────

    private void setupAlgorithmCombo() {
        algorithmCombo.setItems(FXCollections.observableArrayList(AlgorithmRegistry.displayNames()));
        algorithmCombo.getSelectionModel().selectFirst();
        algorithmCombo.setOnAction(e -> {
            lastAlgoIndex = algorithmCombo.getSelectionModel().getSelectedIndex();
            updateQuantumVisibility();
        });
        quantumSpinner.valueProperty().addListener((obs, o, n) -> lastQuantum = n);
    }

    private void updateQuantumVisibility() {
        int idx = algorithmCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= AlgorithmRegistry.ALL.size()) return;
        boolean needs = AlgorithmRegistry.ALL.get(idx).needsQuantum();
        quantumBox.setVisible(needs);
        quantumBox.setManaged(needs);
    }

    // ── Event Handlers ─────────────────────────────────────────────

    @FXML private void onGenerateTable() {
        generateDefaultRows(processCountSpinner.getValue());
        refreshFormUI();
        toast.showSuccess("Generated " + processCountSpinner.getValue() + " process rows.");
    }

    @FXML private void onRunSimulation() {
        persistedRows.setAll(processRows);
        lastAlgoIndex = algorithmCombo.getSelectionModel().getSelectedIndex();
        lastQuantum   = quantumSpinner.getValue();

        List<Process> processes = validateAndBuild();
        if (processes == null) return;

        AlgorithmRegistry.AlgorithmMeta meta = AlgorithmRegistry.get(lastAlgoIndex);
        int quantum = quantumSpinner.getValue();

        try {
            if (meta.id() == AlgoId.COMPARE_ALL) {
                ResultController.setCompareResults(AlgorithmRegistry.runAll(processes, quantum));
            } else {
                ResultController.setSingleResult(AlgorithmRegistry.run(meta.id(), processes, quantum));
            }
            App.setRoot("results");
        } catch (Exception ex) {
            toast.showError("Error: " + ex.getMessage());
        }
    }

    @FXML private void onReset() {
        persistedRows.clear();
        processCountSpinner.getValueFactory().setValue(5);
        algorithmCombo.getSelectionModel().select(0);
        quantumSpinner.getValueFactory().setValue(2);
        generateDefaultRows(5);
        refreshFormUI();
        updateQuantumVisibility();
        toast.showSuccess("Reset complete.");
    }

    // ── Helpers ────────────────────────────────────────────────────

    private List<Process> validateAndBuild() {
        if (processRows.isEmpty()) { toast.showError("Generate the process table first."); return null; }
        List<Process> out = new ArrayList<>();
        for (ProcessRow row : processRows) {
            if (row.getBurstTime() <= 0)  { toast.showError("Burst time must be > 0.");          return null; }
            if (row.getArrivalTime() < 0) { toast.showError("Arrival time cannot be negative."); return null; }
            out.add(row.toProcess());
        }
        return out;
    }

    private void generateDefaultRows(int count) {
        processRows.clear();
        for (int i = 0; i < count; i++) {
            processRows.add(new ProcessRow("P" + (i + 1), i, 5, i + 1));
        }
    }
}
