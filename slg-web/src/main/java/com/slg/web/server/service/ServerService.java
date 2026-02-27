package com.slg.web.server.service;

import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.model.GameServerZkInfo;
import com.slg.net.zookeeper.model.ZKConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 服务器管理与导量服务
 * 基于 ZKConfig 获取 GameServer 信息，提供推荐服分配、导量判断等能力
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class ServerService {

    @Autowired
    private ZKConfig zkConfig;

    /**
     * 根据 serverId 获取 GameServer 信息
     *
     * @return GameServerZkInfo，不存在返回 null
     */
    public GameServerZkInfo getById(int serverId) {
        return zkConfig.getGameServer(serverId);
    }

    /**
     * 获取所有 GameServer 列表（按 serverId 排序）
     */
    public List<GameServerZkInfo> getAll() {
        Map<Integer, GameServerZkInfo> servers = zkConfig.getAllGameServers();
        List<GameServerZkInfo> list = new ArrayList<>(servers.values());
        list.sort(Comparator.comparingInt(GameServerZkInfo::getServerId));
        return list;
    }

    /**
     * 为新用户推荐服务器
     * 简化版：选择导量开关为 open 或 auto 且在线的最小 serverId 服务器
     * 后续可扩展按 country、bundleId 精细匹配导量配置
     *
     * @param country  国家/地区代码
     * @param bundleId 包名
     * @return 推荐的 GameServerZkInfo，未找到返回 null
     */
    public GameServerZkInfo getBestRecommendServer(String country, String bundleId) {
        List<GameServerZkInfo> candidates = getAll().stream()
                .filter(GameServerZkInfo::isAlive)
                .filter(GameServerZkInfo::isEnable)
                .filter(this::isDiversionOpen)
                .toList();

        if (candidates.isEmpty()) {
            LoggerUtil.error("[ServerService] 未找到可用的导量目标服务器, country={}, bundleId={}", country, bundleId);
            return null;
        }

        return candidates.getFirst();
    }

    /**
     * 获取最后一个可用的运行中服务器（推荐服未找到时的兜底）
     */
    public GameServerZkInfo getLastServer() {
        List<GameServerZkInfo> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            GameServerZkInfo info = all.get(i);
            if (info.isAlive() && info.isEnable()) {
                return info;
            }
        }
        return null;
    }

    /**
     * 判断服务器导量是否开放
     * close = 关闭导量，open = 开放导量，auto = 自动判断（当前简化为开放）
     */
    private boolean isDiversionOpen(GameServerZkInfo info) {
        String sw = info.getDiversionSwitch();
        if (sw == null) {
            return false;
        }
        return "open".equals(sw) || "auto".equals(sw);
    }
}
