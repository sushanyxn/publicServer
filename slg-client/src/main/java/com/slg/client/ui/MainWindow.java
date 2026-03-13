package com.slg.client.ui;

import com.slg.client.core.account.AccountManager;
import com.slg.client.core.account.ClientAccount;
import com.slg.client.core.module.ClientModuleManager;
import com.slg.client.core.module.IClientModule;
import com.slg.client.ui.panel.LoginPanel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端模拟器主窗口
 * 包含顶部工具栏（新增账号）、左侧模块导航、右侧内容区域、底部账号标签页
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class MainWindow {

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private ClientModuleManager moduleManager;

    private TabPane accountTabPane;
    private final Map<ClientAccount, AccountTab> accountTabs = new HashMap<>();

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-container");

        HBox topBar = createTopBar();
        root.setTop(topBar);

        accountTabPane = new TabPane();
        accountTabPane.getStyleClass().add("account-tabs");
        accountTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        accountTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                ClientAccount account = (ClientAccount) newTab.getUserData();
                if (account != null) {
                    accountManager.switchTo(account);
                }
            }
        });

        showLoginTab();

        root.setCenter(accountTabPane);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/client.css").toExternalForm());

        stage.setTitle("SLG 客户端模拟器");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * 显示初始登录标签页
     */
    private void showLoginTab() {
        Tab loginTab = new Tab("新建连接");
        loginTab.setClosable(false);
        LoginPanel loginPanel = new LoginPanel(accountManager, this::onLoginInitiated);
        loginTab.setContent(loginPanel.create());
        accountTabPane.getTabs().add(loginTab);
    }

    /**
     * 登录发起后的回调：创建账号标签页
     */
    private void onLoginInitiated(ClientAccount account) {
        Platform.runLater(() -> {
            AccountTab accountTab = new AccountTab(account);
            accountTabs.put(account, accountTab);

            Tab tab = accountTab.getTab();
            tab.setOnClosed(e -> {
                accountTabs.remove(account);
                ClientAccount current = accountManager.getByAccountName(account.getAccount());
                if (current == account && account.isConnected()) {
                    accountManager.disconnect(account.getAccount());
                }
            });

            int insertIndex = accountTabPane.getTabs().size() - 1;
            accountTabPane.getTabs().add(insertIndex, tab);
            accountTabPane.getSelectionModel().select(tab);
        });
    }

    /**
     * 登录成功后刷新账号标签页内容
     */
    public void onLoginSuccess(ClientAccount account) {
        Platform.runLater(() -> {
            AccountTab accountTab = accountTabs.get(account);
            if (accountTab != null) {
                accountTab.switchToMainView();
                accountTab.getTab().setText(account.toString());
            }
        });
    }

    private HBox createTopBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("top-toolbar");

        Label titleLabel = new Label("SLG 客户端模拟器");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ 新建连接");
        addBtn.setOnAction(e -> {
            Tab lastTab = accountTabPane.getTabs().getLast();
            accountTabPane.getSelectionModel().select(lastTab);
        });

        bar.getChildren().addAll(titleLabel, spacer, addBtn);
        return bar;
    }

    /**
     * 每个账号对应一个标签页，内部包含导航和内容区
     */
    private class AccountTab {
        private final ClientAccount account;
        @lombok.Getter
        private final Tab tab;
        private final BorderPane contentPane;
        private final StackPane rightContent;

        AccountTab(ClientAccount account) {
            this.account = account;
            this.tab = new Tab(account.getAccount() + " [连接中...]");
            this.tab.setUserData(account);

            this.contentPane = new BorderPane();
            this.rightContent = new StackPane();

            Label waitingLabel = new Label("正在连接服务器，请稍候...");
            waitingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            rightContent.getChildren().add(waitingLabel);

            contentPane.setCenter(rightContent);
            tab.setContent(contentPane);
        }

        void switchToMainView() {
            ListView<String> navList = new ListView<>();
            navList.getStyleClass().add("nav-list");
            navList.setPrefWidth(160);

            ObservableList<String> navItems = FXCollections.observableArrayList();
            for (IClientModule module : moduleManager.getSortedModules()) {
                navItems.add(module.moduleName());
            }
            navList.setItems(navItems);

            navList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
                int index = newIdx.intValue();
                if (index >= 0 && index < moduleManager.getSortedModules().size()) {
                    IClientModule module = moduleManager.getSortedModules().get(index);
                    Pane panel = module.createPanel(account);
                    panel.getStyleClass().add("content-area");
                    rightContent.getChildren().setAll(panel);
                }
            });

            SplitPane splitPane = new SplitPane();
            splitPane.setOrientation(Orientation.HORIZONTAL);
            splitPane.getItems().addAll(navList, rightContent);
            splitPane.setDividerPositions(0.15);

            contentPane.setCenter(splitPane);

            if (!navItems.isEmpty()) {
                navList.getSelectionModel().selectFirst();
            }
        }
    }
}
