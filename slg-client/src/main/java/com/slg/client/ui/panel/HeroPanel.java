package com.slg.client.ui.panel;

import com.slg.client.config.ClientConfigManager;
import com.slg.client.config.ClientHeroTable;
import com.slg.client.core.account.ClientAccount;
import com.slg.client.core.module.IClientModule;
import com.slg.client.message.hero.HeroClientHandler;
import com.slg.net.message.clientmessage.hero.packet.CM_HeroLevelUp;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 英雄模块面板
 * 展示玩家拥有的英雄列表，支持升级操作
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class HeroPanel implements IClientModule {

    @Autowired
    private ClientConfigManager configManager;

    @Override
    public String moduleName() {
        return "英雄";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Pane createPanel(ClientAccount account) {
        VBox root = new VBox(12);
        root.getStyleClass().add("info-panel");
        root.setPadding(new Insets(20));

        Label sectionTitle = new Label("英雄列表");
        sectionTitle.getStyleClass().add("section-title");

        Label hint = new Label("登录后自动获取英雄数据");
        hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        FlowPane heroGrid = new FlowPane(12, 12);
        heroGrid.setPadding(new Insets(8, 0, 0, 0));

        ScrollPane scrollPane = new ScrollPane(heroGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        @SuppressWarnings("unchecked")
        ObservableList<HeroVO> heroList = account.getModuleData(HeroClientHandler.MODULE_KEY, ObservableList.class);
        if (heroList == null) {
            heroList = FXCollections.observableArrayList();
            account.setModuleData(HeroClientHandler.MODULE_KEY, heroList);
        }

        rebuildHeroCards(heroGrid, heroList, account);
        hint.setText(heroList.isEmpty() ? "等待服务器下发英雄数据..." : "共 " + heroList.size() + " 个英雄");

        ObservableList<HeroVO> finalList = heroList;
        heroList.addListener((ListChangeListener<HeroVO>) change -> {
            rebuildHeroCards(heroGrid, finalList, account);
            hint.setText("共 " + finalList.size() + " 个英雄");
        });

        root.getChildren().addAll(sectionTitle, hint, scrollPane);
        return root;
    }

    private void rebuildHeroCards(FlowPane container, ObservableList<HeroVO> heroList, ClientAccount account) {
        container.getChildren().clear();
        for (HeroVO hero : heroList) {
            container.getChildren().add(createHeroCard(hero, account));
        }
    }

    private VBox createHeroCard(HeroVO hero, ClientAccount account) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setPrefWidth(140);
        card.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-border-color: #bdc3c7;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
        );

        String heroName = resolveHeroName(hero.getHeroId());
        Label nameLabel = new Label(heroName);
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #8e44ad;");

        Label idLabel = new Label("#" + hero.getHeroId());
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Label lvLabel = new Label("Lv." + hero.getHeroLv());
        lvLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");

        Button levelUpBtn = new Button("升 级");
        levelUpBtn.setStyle(
                "-fx-background-color: #3498db;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 4 16;" +
                "-fx-background-radius: 4;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 12px;"
        );
        levelUpBtn.setOnAction(e -> {
            CM_HeroLevelUp req = new CM_HeroLevelUp();
            req.setHeroId(hero.getHeroId());
            account.sendMessage(req);
        });

        card.getChildren().addAll(nameLabel, idLabel, lvLabel, levelUpBtn);
        return card;
    }

    private String resolveHeroName(int heroId) {
        ClientHeroTable heroTable = configManager.getHeroTable(heroId);
        if (heroTable != null && heroTable.getName() != null) {
            return heroTable.getName();
        }
        return "英雄 #" + heroId;
    }
}
