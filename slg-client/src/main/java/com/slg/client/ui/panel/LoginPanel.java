package com.slg.client.ui.panel;

import com.slg.client.core.ClientException;
import com.slg.client.core.account.AccountManager;
import com.slg.client.core.account.ClientAccount;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * 登录面板
 * 提供账号和玩家 ID 的输入，触发登录流程
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public class LoginPanel {

    private final AccountManager accountManager;
    private final Consumer<ClientAccount> onLoginInitiated;

    public LoginPanel(AccountManager accountManager, Consumer<ClientAccount> onLoginInitiated) {
        this.accountManager = accountManager;
        this.onLoginInitiated = onLoginInitiated;
    }

    /**
     * 创建登录面板 UI
     */
    public Pane create() {
        VBox root = new VBox(20);
        root.getStyleClass().add("login-panel");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("SLG 客户端模拟器");
        title.getStyleClass().add("title");

        Label subtitle = new Label("输入账号连接到游戏服务器");
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        TextField accountField = new TextField();
        accountField.setPromptText("账号名称");
        accountField.getStyleClass().add("text-field");

        TextField playerIdField = new TextField();
        playerIdField.setPromptText("玩家 ID（可选，0 表示新登录）");
        playerIdField.getStyleClass().add("text-field");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");

        Button loginBtn = new Button("登 录");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setOnAction(e -> {
            String account = accountField.getText().trim();
            if (account.isEmpty()) {
                statusLabel.setText("请输入账号名称");
                return;
            }

            long playerId = 0;
            String pidText = playerIdField.getText().trim();
            if (!pidText.isEmpty()) {
                try {
                    playerId = Long.parseLong(pidText);
                } catch (NumberFormatException ex) {
                    statusLabel.setText("玩家 ID 必须为数字");
                    return;
                }
            }

            try {
                ClientAccount clientAccount = accountManager.login(account, playerId);
                if (onLoginInitiated != null) {
                    onLoginInitiated.accept(clientAccount);
                }
                accountField.clear();
                playerIdField.clear();
                statusLabel.setText("");
            } catch (ClientException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        root.getChildren().addAll(title, subtitle, accountField, playerIdField, loginBtn, statusLabel);
        return root;
    }
}
