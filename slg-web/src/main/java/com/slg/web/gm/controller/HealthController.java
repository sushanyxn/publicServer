package com.slg.web.gm.controller;

import com.slg.net.zookeeper.model.GameServerZkInfo;
import com.slg.net.zookeeper.model.SceneServerZkInfo;
import com.slg.net.zookeeper.model.ZKConfig;
import com.slg.web.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * 服务健康检查控制器
 * 通过 ZKConfig 中的 alive 状态检查服务器存活
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Controller
@RequestMapping("/gm/health")
public class HealthController {

    @Autowired
    private ZKConfig zkConfig;

    /**
     * 健康检查页面
     */
    @GetMapping("/page")
    public String healthPage() {
        return "console/health";
    }

    /**
     * Web 服自身健康检查（公开接口，不需要登录）
     */
    @GetMapping("")
    @ResponseBody
    public Response<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "slg-web");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        status.put("gameServers", zkConfig.getAllGameServers().size());
        status.put("sceneServers", zkConfig.getAllSceneServers().size());
        return Response.success(status);
    }

    /**
     * 所有服务器存活状态概览
     */
    @GetMapping("/overview")
    @ResponseBody
    public Response<Map<String, Object>> overview() {
        Map<Integer, GameServerZkInfo> gameServers = zkConfig.getAllGameServers();
        Map<Integer, SceneServerZkInfo> sceneServers = zkConfig.getAllSceneServers();

        int totalGame = gameServers.size();
        int aliveGame = (int) gameServers.values().stream().filter(GameServerZkInfo::isAlive).count();
        int totalScene = sceneServers.size();
        int aliveScene = (int) sceneServers.values().stream().filter(SceneServerZkInfo::isAlive).count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("gameServers", Map.of("total", totalGame, "alive", aliveGame));
        overview.put("sceneServers", Map.of("total", totalScene, "alive", aliveScene));

        List<Map<String, Object>> details = new ArrayList<>();
        for (GameServerZkInfo info : gameServers.values()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("serverId", info.getServerId());
            detail.put("type", "GAME");
            detail.put("alive", info.isAlive());
            detail.put("enable", info.isEnable());
            details.add(detail);
        }
        for (SceneServerZkInfo info : sceneServers.values()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("serverId", info.getServerId());
            detail.put("type", "SCENE");
            detail.put("alive", info.isAlive());
            detail.put("enable", info.isEnable());
            details.add(detail);
        }
        details.sort(Comparator.<Map<String, Object>, Integer>comparing(m -> (int) m.get("serverId"))
                .thenComparing(m -> (String) m.get("type")));
        overview.put("details", details);

        return Response.success(overview);
    }
}
