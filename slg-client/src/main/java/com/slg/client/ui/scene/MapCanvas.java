package com.slg.client.ui.scene;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SLG 大地图画布
 * 支持平移、缩放、网格绘制和场景实体渲染
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public class MapCanvas extends Canvas {

    private static final double CELL_SIZE = 40;
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_FACTOR = 1.1;

    private double cameraX = 0;
    private double cameraY = 0;
    private double zoom = 1.0;

    private double dragStartX, dragStartY;
    private double dragCameraStartX, dragCameraStartY;
    private boolean dragging;

    private final Map<Long, MapEntity> entities = new ConcurrentHashMap<>();

    private Consumer<MapClickEvent> onEntityClick;

    public MapCanvas(double width, double height) {
        super(width, height);
        setupEventHandlers();

        widthProperty().addListener(e -> render());
        heightProperty().addListener(e -> render());
    }

    private void setupEventHandlers() {
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(this::onMouseReleased);
        setOnScroll(this::onScroll);
    }

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragCameraStartX = cameraX;
            dragCameraStartY = cameraY;
            dragging = false;
        }
    }

    private void onMouseDragged(MouseEvent e) {
        double dx = e.getX() - dragStartX;
        double dy = e.getY() - dragStartY;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            dragging = true;
        }
        cameraX = dragCameraStartX - dx / zoom;
        cameraY = dragCameraStartY - dy / zoom;
        render();
    }

    private void onMouseReleased(MouseEvent e) {
        if (!dragging && e.getButton() == MouseButton.PRIMARY) {
            double worldX = cameraX + (e.getX() - getWidth() / 2) / zoom;
            double worldY = cameraY + (e.getY() - getHeight() / 2) / zoom;

            int cellX = (int) Math.floor(worldX / CELL_SIZE);
            int cellY = (int) Math.floor(worldY / CELL_SIZE);

            MapEntity clicked = findEntityAt(cellX, cellY);
            if (onEntityClick != null) {
                onEntityClick.accept(new MapClickEvent(cellX, cellY, clicked));
            }
        }
    }

    private void onScroll(ScrollEvent e) {
        double oldZoom = zoom;
        if (e.getDeltaY() > 0) {
            zoom = Math.min(MAX_ZOOM, zoom * ZOOM_FACTOR);
        } else {
            zoom = Math.max(MIN_ZOOM, zoom / ZOOM_FACTOR);
        }

        double mouseWorldXBefore = cameraX + (e.getX() - getWidth() / 2) / oldZoom;
        double mouseWorldYBefore = cameraY + (e.getY() - getHeight() / 2) / oldZoom;
        double mouseWorldXAfter = cameraX + (e.getX() - getWidth() / 2) / zoom;
        double mouseWorldYAfter = cameraY + (e.getY() - getHeight() / 2) / zoom;

        cameraX += mouseWorldXBefore - mouseWorldXAfter;
        cameraY += mouseWorldYBefore - mouseWorldYAfter;

        render();
    }

    /**
     * 渲染场景
     */
    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, w, h);

        gc.save();
        gc.translate(w / 2 - cameraX * zoom, h / 2 - cameraY * zoom);
        gc.scale(zoom, zoom);

        drawGrid(gc, w, h);
        drawEntities(gc);

        gc.restore();

        drawHUD(gc, w, h);
    }

    private void drawGrid(GraphicsContext gc, double viewWidth, double viewHeight) {
        double halfW = viewWidth / 2 / zoom;
        double halfH = viewHeight / 2 / zoom;

        int startX = (int) Math.floor((cameraX - halfW) / CELL_SIZE) - 1;
        int endX = (int) Math.ceil((cameraX + halfW) / CELL_SIZE) + 1;
        int startY = (int) Math.floor((cameraY - halfH) / CELL_SIZE) - 1;
        int endY = (int) Math.ceil((cameraY + halfH) / CELL_SIZE) + 1;

        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(0.5);

        for (int x = startX; x <= endX; x++) {
            double px = x * CELL_SIZE;
            gc.strokeLine(px, startY * CELL_SIZE, px, endY * CELL_SIZE);
        }
        for (int y = startY; y <= endY; y++) {
            double py = y * CELL_SIZE;
            gc.strokeLine(startX * CELL_SIZE, py, endX * CELL_SIZE, py);
        }

        if (zoom > 0.5) {
            gc.setFill(Color.web("#0f3460", 0.6));
            gc.setFont(Font.font(9));
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    if ((x + y) % 5 == 0) {
                        gc.fillText(x + "," + y, x * CELL_SIZE + 2, y * CELL_SIZE + 10);
                    }
                }
            }
        }
    }

    private void drawEntities(GraphicsContext gc) {
        for (MapEntity entity : entities.values()) {
            double px = entity.getX() * CELL_SIZE;
            double py = entity.getY() * CELL_SIZE;

            gc.setFill(entity.getColor());
            gc.fillRect(px + 2, py + 2, CELL_SIZE - 4, CELL_SIZE - 4);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(10));
            gc.fillText(entity.getLabel(), px + 4, py + CELL_SIZE / 2 + 3);
        }
    }

    private void drawHUD(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.web("#ffffff", 0.8));
        gc.setFont(Font.font(12));

        int cx = (int) Math.floor(cameraX / CELL_SIZE);
        int cy = (int) Math.floor(cameraY / CELL_SIZE);
        String info = String.format("坐标: (%d, %d)  缩放: %.0f%%  实体: %d",
                cx, cy, zoom * 100, entities.size());
        gc.fillText(info, 10, h - 10);
    }

    private MapEntity findEntityAt(int cellX, int cellY) {
        for (MapEntity entity : entities.values()) {
            if (entity.getX() == cellX && entity.getY() == cellY) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 添加或更新场景实体
     */
    public void putEntity(MapEntity entity) {
        entities.put(entity.getId(), entity);
    }

    /**
     * 移除场景实体
     */
    public void removeEntity(long entityId) {
        entities.remove(entityId);
    }

    /**
     * 清空所有实体
     */
    public void clearEntities() {
        entities.clear();
    }

    /**
     * 将视口移动到指定坐标
     */
    public void moveTo(int x, int y) {
        cameraX = x * CELL_SIZE + CELL_SIZE / 2;
        cameraY = y * CELL_SIZE + CELL_SIZE / 2;
        render();
    }

    public void setOnEntityClick(Consumer<MapClickEvent> handler) {
        this.onEntityClick = handler;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /**
     * 地图点击事件
     */
    public record MapClickEvent(int cellX, int cellY, MapEntity entity) {}
}
