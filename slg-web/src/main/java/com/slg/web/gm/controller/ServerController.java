package com.slg.web.gm.controller;

import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.model.GameServerZkInfo;
import com.slg.net.zookeeper.model.SceneServerZkInfo;
import com.slg.net.zookeeper.model.ZKConfig;
import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.web.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * GM 服务器管理控制器
 * 数据来源为 ZKConfig，无 RPC 依赖
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Controller
@RequestMapping("/gm/server")
public class ServerController {

    @Autowired
    private ZKConfig zkConfig;

    @Autowired
    private ZookeeperShareService zookeeperShareService;

    /**
     * 服务器管理页面
     */
    @GetMapping("/page")
    public String serverPage() {
        return "console/server";
    }

    /**
     * 获取所有 GameServer 列表（JSON）
     */
    @GetMapping("/list")
    @ResponseBody
    public Response<List<Map<String, Object>>> serverList() {
        Map<Integer, GameServerZkInfo> gameServers = zkConfig.getAllGameServers();
        Map<Integer, SceneServerZkInfo> sceneServers = zkConfig.getAllSceneServers();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, GameServerZkInfo> entry : gameServers.entrySet()) {
            GameServerZkInfo info = entry.getValue();
            Map<String, Object> serverData = new LinkedHashMap<>();
            serverData.put("serverId", info.getServerId());
            serverData.put("gameHost", info.getGameHost());
            serverData.put("gamePort", info.getGamePort());
            serverData.put("rpcPort", info.getRpcPort());
            serverData.put("enable", info.isEnable());
            serverData.put("inServerList", info.isInServerList());
            serverData.put("alive", info.isAlive());
            serverData.put("openTimeMs", info.getOpenTimeMs());
            serverData.put("registedRole", info.getRegistedRole());
            serverData.put("diversionSwitch", info.getDiversionSwitch());
            serverData.put("multiRoleServerShow", info.isMultiRoleServerShow());

            SceneServerZkInfo sceneInfo = sceneServers.get(info.getServerId());
            serverData.put("sceneAlive", sceneInfo != null && sceneInfo.isAlive());

            result.add(serverData);
        }

        result.sort(Comparator.comparingInt(m -> (int) m.get("serverId")));
        return Response.success(result);
    }

    /**
     * 获取单个服务器详情
     */
    @GetMapping("/detail/{serverId}")
    @ResponseBody
    public Response<Map<String, Object>> serverDetail(@PathVariable int serverId) {
        GameServerZkInfo info = zkConfig.getGameServer(serverId);
        if (info == null) {
            return Response.error(1004, "服务器不存在: " + serverId);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("serverId", info.getServerId());
        detail.put("gameIp", info.getGameIp());
        detail.put("gameHost", info.getGameHost());
        detail.put("gamePort", info.getGamePort());
        detail.put("rpcIp", info.getRpcIp());
        detail.put("rpcPort", info.getRpcPort());
        detail.put("enable", info.isEnable());
        detail.put("inServerList", info.isInServerList());
        detail.put("alive", info.isAlive());
        detail.put("openTimeMs", info.getOpenTimeMs());
        detail.put("registedRole", info.getRegistedRole());
        detail.put("dbVersion", info.getDbVersion());
        detail.put("timeZoneOffset", info.getTimeZoneOffset());
        detail.put("mergeServerVersion", info.getMergeServerVersion());
        detail.put("diversionConfig", info.getDiversionConfig());
        detail.put("diversionSwitch", info.getDiversionSwitch());
        detail.put("multiRoleServerShow", info.isMultiRoleServerShow());

        return Response.success(detail);
    }

    /**
     * 修改导量开关
     *
     * @param serverId        服务器 ID
     * @param diversionSwitch 导量开关值：close / open / auto
     */
    @PostMapping("/diversion")
    @ResponseBody
    public Response<?> updateDiversionSwitch(@RequestParam int serverId,
                                             @RequestParam String diversionSwitch) {
        GameServerZkInfo info = zkConfig.getGameServer(serverId);
        if (info == null) {
            return Response.error(1004, "服务器不存在: " + serverId);
        }

        if (!"close".equals(diversionSwitch) && !"open".equals(diversionSwitch)
                && !"auto".equals(diversionSwitch)) {
            return Response.error(1001, "无效的导量开关值，允许值: close/open/auto");
        }

        zookeeperShareService.writeGameServerField(serverId, "diversion_switch", diversionSwitch);
        LoggerUtil.debug("[GM] 修改服务器 {} 导量开关为: {}", serverId, diversionSwitch);
        return Response.success();
    }
}
