package com.slg.client.ui.panel;

import com.slg.client.core.account.ClientAccount;
import com.slg.client.core.module.IClientModule;
import com.slg.client.message.gm.GMClientHandler;
import com.slg.net.message.clientmessage.gm.packet.CM_GMCommand;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

/**
 * GM 指令面板
 * 提供指令输入框、常用快捷按钮和结果日志区域
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class GMPanel implements IClientModule {

    private static final String[][] QUICK_COMMANDS = {
            {"获得全部英雄", "gainAllHero"},
            {"英雄满级", "heroMaxLevel"},
            {"获得英雄1001", "gainHero 1001"},
            {"设置英雄等级", "setHeroLevel 1001 3"},
    };

    @Override
    public String moduleName() {
        return "GM 指令";
    }

    @Override
    public int order() {
        return 999;
    }

    @Override
    public Pane createPanel(ClientAccount account) {
        VBox root = new VBox(12);
        root.getStyleClass().add("info-panel");
        root.setPadding(new Insets(20));

        Label sectionTitle = new Label("GM 指令");
        sectionTitle.getStyleClass().add("section-title");

        // --- 输入区 ---
        TextField inputField = new TextField();
        inputField.setPromptText("输入 GM 指令，如: gainHero 1001");
        inputField.setStyle("-fx-pref-height: 32px; -fx-font-size: 13px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("发 送");
        sendBtn.setStyle(
                "-fx-background-color: #27ae60;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 6 20;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;"
        );

        HBox inputRow = new HBox(8, inputField, sendBtn);
        inputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // --- 快捷按钮区 ---
        Label quickTitle = new Label("常用指令:");
        quickTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        FlowPane quickPane = new FlowPane(8, 6);
        for (String[] cmd : QUICK_COMMANDS) {
            Button btn = new Button(cmd[0]);
            btn.setStyle(
                    "-fx-background-color: #ecf0f1;" +
                    "-fx-text-fill: #2c3e50;" +
                    "-fx-padding: 4 12;" +
                    "-fx-background-radius: 4;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 11px;"
            );
            String command = cmd[1];
            btn.setOnAction(e -> {
                inputField.setText(command);
                sendCommand(account, command);
            });
            quickPane.getChildren().add(btn);
        }

        VBox quickBox = new VBox(4, quickTitle, quickPane);

        // --- 日志区 ---
        Label logTitle = new Label("执行日志:");
        logTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-padding: 8 0 0 0;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        SimpleStringProperty logProperty = GMClientHandler.getOrCreateLog(account);
        logArea.setText(logProperty.get());
        logProperty.addListener((obs, oldVal, newVal) ->
                Platform.runLater(() -> {
                    logArea.setText(newVal);
                    logArea.setScrollTop(Double.MAX_VALUE);
                })
        );

        Runnable doSend = () -> {
            String cmd = inputField.getText().trim();
            if (!cmd.isEmpty()) {
                sendCommand(account, cmd);
                inputField.clear();
            }
        };
        sendBtn.setOnAction(e -> doSend.run());
        inputField.setOnAction(e -> doSend.run());

        Button clearBtn = new Button("清空日志");
        clearBtn.setStyle(
                "-fx-background-color: #e74c3c;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 4 12;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 11px;"
        );
        clearBtn.setOnAction(e -> logProperty.set(""));

        HBox logHeader = new HBox(8, logTitle, new Region(), clearBtn);
        logHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(logHeader.getChildren().get(1), Priority.ALWAYS);

        root.getChildren().addAll(sectionTitle, inputRow, quickBox, logHeader, logArea);
        return root;
    }

    private void sendCommand(ClientAccount account, String command) {
        SimpleStringProperty logProperty = GMClientHandler.getOrCreateLog(account);
        logProperty.set(logProperty.get() + "> " + command + "\n");

        CM_GMCommand packet = new CM_GMCommand();
        packet.setCommand(command);
        account.sendMessage(packet);
    }

}
