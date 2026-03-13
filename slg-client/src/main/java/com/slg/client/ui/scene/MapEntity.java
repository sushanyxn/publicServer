package com.slg.client.ui.scene;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

/**
 * 地图实体
 * 表示大地图上的一个可渲染对象（城池、部队、资源点等）
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
public class MapEntity {

    private long id;
    private int x;
    private int y;
    private String label;
    private Color color;
    private EntityType type;

    public enum EntityType {
        CITY(Color.web("#e74c3c")),
        ARMY(Color.web("#3498db")),
        RESOURCE(Color.web("#27ae60")),
        NPC(Color.web("#9b59b6")),
        OTHER(Color.web("#95a5a6"));

        @Getter
        private final Color defaultColor;

        EntityType(Color defaultColor) {
            this.defaultColor = defaultColor;
        }
    }

    public static MapEntity valueOf(long id, int x, int y, String label, EntityType type) {
        MapEntity entity = new MapEntity();
        entity.id = id;
        entity.x = x;
        entity.y = y;
        entity.label = label;
        entity.type = type;
        entity.color = type.getDefaultColor();
        return entity;
    }
}
