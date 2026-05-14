package com.cpusched;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Lightweight toast notification system.
 * Attach a StackPane overlay to your root, then call show().
 */
public class ToastNotification {

    private static final String STYLE_SUCCESS =
        "-fx-background-color: #f0e4d0; -fx-border-color: #4ecca3; -fx-border-width: 1;" +
        "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 10 16;";
    private static final String STYLE_ERROR =
        "-fx-background-color: #f0e4d0; -fx-border-color: #e94560; -fx-border-width: 1;" +
        "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 10 16;";
    private static final String TEXT_SUCCESS = "-fx-text-fill: #5c3317; -fx-font-family: Consolas; -fx-font-size: 12;";
    private static final String TEXT_ERROR   = "-fx-text-fill: #5c3317; -fx-font-family: Consolas; -fx-font-size: 12;";

    private final StackPane overlay;

    public ToastNotification(StackPane overlay) {
        this.overlay = overlay;
    }

    public void showSuccess(String message) { show("✓  " + message, STYLE_SUCCESS, TEXT_SUCCESS); }
    public void showError(String message)   { show("✗  " + message, STYLE_ERROR,   TEXT_ERROR);   }

    private void show(String message, String boxStyle, String textStyle) {
        Label label = new Label(message);
        label.setStyle(textStyle);

        VBox toast = new VBox(label);
        toast.setStyle(boxStyle);
        toast.setAlignment(Pos.CENTER);
        toast.setMaxWidth(300);
        toast.setMaxHeight(50);
        toast.setOpacity(0);
        toast.setMouseTransparent(true);

        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 20, 20, 0));

        overlay.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.seconds(2.5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> overlay.getChildren().remove(toast));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }
}
