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

/**
 * Canvas-based Gantt chart with:
 *  - Proportional colored blocks per process
 *  - Drop shadows
 *  - Vertical grid lines at each time tick
 *  - Tooltips on hover showing process + time range
 */
public class GanttCanvas extends VBox {

    private static final double BLOCK_HEIGHT  = 52;
    private static final double TIMELINE_H    = 22;
    private static final double TOTAL_WIDTH   = 700;
    private static final double SHADOW_BLUR   = 6;
    private static final Color  GRID_COLOR    = Color.web("#2a1a0a", 0.6);
    private static final Color  TIMELINE_CLR  = Color.web("#7a7a9d");
    private static final Font   LABEL_FONT    = Font.font("Consolas", FontWeight.BOLD, 12);
    private static final Font   TIME_FONT     = Font.font("Consolas", 10);

    public GanttCanvas(List<GanttEntry> gantt, Map<String, String> colorMap) {
        setPadding(new Insets(4));
        setSpacing(0);

        if (gantt == null || gantt.isEmpty()) return;

        int totalTime = gantt.get(gantt.size() - 1).getEndTime();
        if (totalTime <= 0) return;

        Canvas canvas = new Canvas(TOTAL_WIDTH, BLOCK_HEIGHT + TIMELINE_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawChart(gc, gantt, colorMap, totalTime);
        addTooltips(canvas, gantt, colorMap, totalTime);

        // Wrap in a Pane so it doesn't stretch
        Pane wrapper = new Pane(canvas);
        wrapper.setPrefSize(TOTAL_WIDTH, BLOCK_HEIGHT + TIMELINE_H);
        getChildren().add(wrapper);
    }

    private void drawChart(GraphicsContext gc, List<GanttEntry> gantt,
                           Map<String, String> colorMap, int totalTime) {

        // Background
        gc.setFill(Color.web("#1c1008"));
        gc.fillRect(0, 0, TOTAL_WIDTH, BLOCK_HEIGHT);

        double x = 0;
        for (GanttEntry entry : gantt) {
            double w = blockWidth(entry.getDuration(), totalTime);
            if (w < 1) w = 1;

            Color fill = ProcessColors.color(colorMap.getOrDefault(entry.getProcessId(), "#555"));
            boolean isIdle = entry.getProcessId().equals("IDLE");

            // Shadow
            gc.setFill(Color.color(0, 0, 0, 0.4));
            gc.fillRoundRect(x + 2, SHADOW_BLUR, w - 2, BLOCK_HEIGHT - SHADOW_BLUR, 5, 5);

            // Block fill
            gc.setFill(fill);
            gc.fillRoundRect(x, 0, w, BLOCK_HEIGHT - 2, 5, 5);

            // Subtle highlight on top
            if (!isIdle) {
                gc.setFill(Color.color(1, 1, 1, 0.12));
                gc.fillRoundRect(x, 0, w, BLOCK_HEIGHT * 0.4, 5, 5);
            }

            // Separator line
            gc.setStroke(Color.web("#1a1a2e"));
            gc.setLineWidth(1);
            gc.strokeLine(x + w, 0, x + w, BLOCK_HEIGHT - 2);

            // Process ID label
            gc.setFill(isIdle ? Color.web("#555580") : Color.WHITE);
            gc.setFont(LABEL_FONT);
            String pid = entry.getProcessId();
            // Clip label if block is too narrow
            if (w > 20) {
                double textX = x + (w / 2) - (pid.length() * 3.8);
                double textY = BLOCK_HEIGHT / 2 + 4;
                gc.fillText(pid, Math.max(x + 2, textX), textY);
            }

            x += w;
        }

        // Draw grid lines at integer time ticks
        drawGridLines(gc, gantt, totalTime);

        // Timeline row
        drawTimeline(gc, gantt, totalTime);
    }

    private void drawGridLines(GraphicsContext gc, List<GanttEntry> gantt, int totalTime) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(0.5);
        gc.setLineDashes(3, 3);

        double x = 0;
        for (GanttEntry e : gantt) {
            gc.strokeLine(x, 0, x, BLOCK_HEIGHT - 2);
            x += blockWidth(e.getDuration(), totalTime);
        }
        gc.strokeLine(x, 0, x, BLOCK_HEIGHT - 2); // final tick
        gc.setLineDashes(null);
    }

    private void drawTimeline(GraphicsContext gc, List<GanttEntry> gantt, int totalTime) {
        gc.setFill(TIMELINE_CLR);
        gc.setFont(TIME_FONT);

        double x = 0;
        for (GanttEntry e : gantt) {
            gc.fillText(String.valueOf(e.getStartTime()), x + 2, BLOCK_HEIGHT + 14);
            x += blockWidth(e.getDuration(), totalTime);
        }
        // Final end time
        String endStr = String.valueOf(gantt.get(gantt.size() - 1).getEndTime());
        gc.fillText(endStr, x - endStr.length() * 5, BLOCK_HEIGHT + 14);
    }

    private void addTooltips(Canvas canvas, List<GanttEntry> gantt,
                             Map<String, String> colorMap, int totalTime) {
        Tooltip tip = new Tooltip();
        tip.setStyle("-fx-font-family: Consolas; -fx-font-size: 11; " +
                     "-fx-background-color: #2e1a0e; -fx-text-fill: #e8c87a; " +
                     "-fx-border-color: #4a2c1a; -fx-border-width: 1;");

        canvas.setOnMouseMoved(evt -> {
            double mx = evt.getX();
            double x = 0;
            for (GanttEntry e : gantt) {
                double w = blockWidth(e.getDuration(), totalTime);
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

    private double blockWidth(int duration, int totalTime) {
        return ((double) duration / totalTime) * TOTAL_WIDTH;
    }
}
