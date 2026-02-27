package com.slg.scene.scene.node.node.service;

import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理场景node
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
public class NodeContainer {

    private Scene scene;

    private Map<Long, SceneNode<?>> sceneNodes = new ConcurrentHashMap<>();

    private Map<Long, RouteNode<?>> routeNodes = new ConcurrentHashMap<>();

    public NodeContainer(Scene scene) {
        this.scene = scene;
    }

    public void addSceneNode(SceneNode<?> sceneNode) {
        sceneNodes.put(sceneNode.getId(), sceneNode);
        if (sceneNode instanceof RouteNode<?> routeNode) {
            routeNodes.put(routeNode.getId(), routeNode);
        }
    }

    public void removeSceneNode(SceneNode<?> sceneNode) {
        sceneNodes.remove(sceneNode.getId());
        if (sceneNode instanceof RouteNode<?> routeNode) {
            routeNodes.remove(routeNode.getId());
        }
    }
}
