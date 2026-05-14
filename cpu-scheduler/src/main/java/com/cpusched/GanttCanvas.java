package com.cpusched;

import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


public class GanttCanvas extends VBox {

    private static final double BLOCK_HEIGHT  = 52;
    private static final double TIMELINE_H    = 22;
    private static final double DEFAULT_WIDTH = 700;
    private static final Color  GRID_COLOR    = Color.web("#2a1a0a", 0.6);
    private static final Color  TIMELINE_CLR  = Color.web("#7a7a9d");
    private static final Font   LABEL_FONT    = Font.font("Consolas", FontWeight.BOLD, 12);
    private static final Font   TIME_FONT     = Font.font("Consolas", 10);

   
    public GanttCanvas(List<GanttEntry> gantt, Map<String, String> colorMap) {
        this(gantt, colorMap, DEFAULT_WIDTH);
    }

    public GanttCanvas(List<GanttEntry> gantt, Map<String, String> colorMap, double totalWidth) {
        setPadding(new Insets(4));
        setSpacing(0);

        if (gantt == null || gantt.isEmpty()) return;

        int totalTime = gantt.get(gantt.size() - 1).getEndTime();
        if (totalTime <= 0) return;

        Canvas canvas = new Canvas(totalWidth, BLOCK_HEIGHT + TIMELINE_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawChart(gc, gantt, colorMap, totalTime, totalWidth);
        addTooltips(canvas, gantt, totalTime, totalWidth);

        Pane wrapper = new Pane(canvas);
        wrapper.setPrefSize(totalWidth, BLOCK_HEIGHT + TIMELINE_H);
        getChildren().add(wrapper);
    }

    private void drawChart(GraphicsContext gc, List<GanttEntry> gantt,
                           Map<String, String> colorMap, int totalTime, double totalWidth) {

        gc.setFill(Color.web("#1c1008"));
        gc.fillRect(0, 0, totalWidth, BLOCK_HEIGHT);

        double x = 0;
        for (GanttEntry entry : gantt) {
            double w = blockWidth(entry.getDuration(), totalTime, totalWidth);
            if (w < 1) w = 1;

            Color fill = ProcessColors.color(colorMap.getOrDefault(entry.getProcessId(), "#555"));
            boolean isIdle = entry.getProcessId().equals("IDLE");

            gc.setFill(Color.color(0, 0, 0, 0.4));
            gc.fillRoundRect(x + 2, 6, w - 2, BLOCK_HEIGHT - 6, 5, 5);

            gc.setFill(fill);
            gc.fillRoundRect(x, 0, w, BLOCK_HEIGHT - 2, 5, 5);

            if (!isIdle) {
                gc.setFill(Color.color(1, 1, 1, 0.12));
                gc.fillRoundRect(x, 0, w, BLOCK_HEIGHT * 0.4, 5, 5);
            }

            gc.setStroke(Color.web("#1a1a2e"));
            gc.setLineWidth(1);
            gc.strokeLine(x + w, 0, x + w, BLOCK_HEIGHT - 2);

            gc.setFill(isIdle ? Color.web("#aaaacc") : Color.WHITE);
            gc.setFont(LABEL_FONT);
            String pid = entry.getProcessId();
            if (w > 20) {
                double textX = x + (w / 2) - (pid.length() * 3.8);
                double textY = BLOCK_HEIGHT / 2 + 4;
                gc.fillText(pid, Math.max(x + 2, textX), textY);
            }

            x += w;
        }

        drawGridLines(gc, gantt, totalTime, totalWidth);
        drawTimeline(gc, gantt, totalTime, totalWidth);
    }

    private void drawGridLines(GraphicsContext gc, List<GanttEntry> gantt, int totalTime, double totalWidth) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(0.5);
        gc.setLineDashes(3, 3);
        double x = 0;
        for (GanttEntry e : gantt) {
            gc.strokeLine(x, 0, x, BLOCK_HEIGHT - 2);
            x += blockWidth(e.getDuration(), totalTime, totalWidth);
        }
        gc.strokeLine(x, 0, x, BLOCK_HEIGHT - 2);
        gc.setLineDashes(null);
    }

    private void drawTimeline(GraphicsContext gc, List<GanttEntry> gantt, int totalTime, double totalWidth) {
        gc.setFill(TIMELINE_CLR);
        gc.setFont(TIME_FONT);
        double x = 0;
        for (GanttEntry e : gantt) {
            gc.fillText(String.valueOf(e.getStartTime()), x + 2, BLOCK_HEIGHT + 14);
            x += blockWidth(e.getDuration(), totalTime, totalWidth);
        }
        String endStr = String.valueOf(gantt.get(gantt.size() - 1).getEndTime());
        gc.fillText(endStr, x - endStr.length() * 5, BLOCK_HEIGHT + 14);
    }

    private void addTooltips(Canvas canvas, List<GanttEntry> gantt, int totalTime, double totalWidth) {
        Tooltip tip = new Tooltip();
        tip.setStyle("-fx-font-family: Consolas; -fx-font-size: 11; " +
                     "-fx-background-color: #2e1a0e; -fx-text-fill: #e8c87a; " +
                     "-fx-border-color: #4a2c1a; -fx-border-width: 1;");

        canvas.setOnMouseMoved(evt -> {
            double mx = evt.getX();
            double x = 0;
            for (GanttEntry e : gantt) {
                double w = blockWidth(e.getDuration(), totalTime, totalWidth);
                if (mx >= x && mx < x + w) {
                    tip.setText(String.format("%s   [%d → %d]   duration: %d",
                        e.getProcessId(), e.getStartTime(), e.getEndTime(), e.getDuration()));
                    Tooltip.install(canvas, tip);
                    return;
                }
                x += w;
            }
            Tooltip.uninstall(canvas, tip);
        });

        canvas.setOnMouseExited(e -> Tooltip.uninstall(canvas, tip));
    }

    private double blockWidth(int duration, int totalTime, double totalWidth) {
        return ((double) duration / totalTime) * totalWidth;
    }
}