package com.slg.client.ui.panel;

import com.slg.client.core.account.ClientAccount;
import com.slg.client.core.module.IClientModule;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

/**
 * 角色基础信息模块
 * 显示当前登录角色的基础信息
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class PlayerInfoPanel implements IClientModule {

    @Override
    public String moduleName() {
        return "角色信息";
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public Pane createPanel(ClientAccount account) {
        VBox root = new VBox(12);
        root.getStyleClass().add("info-panel");
        root.setPadding(new Insets(20));

        Label sectionTitle = new Label("角色基础信息");
        sectionTitle.getStyleClass().add("section-title");

        HBox accountRow = createInfoRow("账号:", account.getAccount());

        Label playerIdValue = new Label(String.valueOf(account.getPlayerId()));
        playerIdValue.getStyleClass().add("info-value");
        account.getPlayerIdProperty().addListener((obs, oldVal, newVal) ->
            Platform.runLater(() -> playerIdValue.setText(String.valueOf(newVal.longValue())))
        );
        HBox playerIdRow = createInfoRow("玩家 ID:", playerIdValue);

        Label statusValue = new Label(account.isLoggedIn() ? "在线" : "离线");
        statusValue.getStyleClass().add(account.isLoggedIn() ? "status-online" : "status-offline");
        account.getLoggedInProperty().addListener((obs, oldVal, newVal) ->
            Platform.runLater(() -> {
                statusValue.setText(newVal ? "在线" : "离线");
                statusValue.getStyleClass().removeAll("status-online", "status-offline");
                statusValue.getStyleClass().add(newVal ? "status-online" : "status-offline");
            })
        );
        HBox statusRow = createInfoRow("状态:", statusValue);

        Label connValue = new Label(account.isConnected() ? "已连接" : "未连接");
        connValue.getStyleClass().add(account.isConnected() ? "status-online" : "status-offline");
        HBox connRow = createInfoRow("连接:", connValue);

        root.getChildren().addAll(sectionTitle, accountRow, playerIdRow, statusRow, connRow);
        return root;
    }

    private HBox createInfoRow(String labelText, String valueText) {
        Label value = new Label(valueText);
        value.getStyleClass().add("info-value");
        return createInfoRow(labelText, value);
    }

    private HBox createInfoRow(String labelText, Label valueLabel) {
        HBox row = new HBox(8);
        row.getStyleClass().add("info-row");

        Label label = new Label(labelText);
        label.getStyleClass().add("info-label");
        label.setMinWidth(80);

        row.getChildren().addAll(label, valueLabel);
        return row;
    }
}
