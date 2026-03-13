package com.slg.client.ui.panel;

import com.slg.client.core.account.ClientAccount;
import com.slg.client.core.module.IClientModule;
import com.slg.client.ui.scene.MapCanvas;
import com.slg.client.ui.scene.MapEntity;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

/**
 * 大地图模块面板
 * 包含 MapCanvas 和底部信息栏
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class MapPanel implements IClientModule {

    @Override
    public String moduleName() {
        return "大地图";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Pane createPanel(ClientAccount account) {
        BorderPane root = new BorderPane();

        MapCanvas canvas = new MapCanvas(800, 600);

        Pane canvasHolder = new Pane(canvas);
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        Label clickInfo = new Label("点击地图查看坐标和实体信息");
        clickInfo.setPadding(new Insets(8, 16, 8, 16));
        clickInfo.setStyle("-fx-background-color: #ecf0f1; -fx-font-size: 12px;");

        canvas.setOnEntityClick(event -> {
            String text = String.format("点击坐标: (%d, %d)", event.cellX(), event.cellY());
            if (event.entity() != null) {
                text += String.format("  实体: %s [%s]", event.entity().getLabel(), event.entity().getType());
            }
            clickInfo.setText(text);
        });

        HBox bottomBar = new HBox(clickInfo);
        bottomBar.setStyle("-fx-background-color: #ecf0f1;");

        root.setCenter(canvasHolder);
        root.setBottom(bottomBar);

        canvas.render();

        return root;
    }
}
