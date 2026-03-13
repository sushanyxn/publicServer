package com.slg.client.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * 客户端提示消息管理器
 * 在主窗口顶部弹出一个自动消失的浮动提示
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public class TipManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * 弹出提示消息，3 秒后自动消失
     */
    public static void showTip(String message) {
        Platform.runLater(() -> {
            if (primaryStage == null || !primaryStage.isShowing()) {
                return;
            }

            Label label = new Label(message);
            label.setStyle(
                    "-fx-background-color: rgba(44,62,80,0.92);" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 6;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;"
            );

            StackPane container = new StackPane(label);
            container.setAlignment(Pos.CENTER);

            Popup popup = new Popup();
            popup.getContent().add(container);
            popup.setAutoHide(true);

            double x = primaryStage.getX() + (primaryStage.getWidth() - 300) / 2;
            double y = primaryStage.getY() + 60;
            popup.show(primaryStage, x, y);

            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> popup.hide());
            pause.play();
        });
    }

}
