package com.slg.client.core.module;

import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 客户端模块管理器
 * 自动扫描并注册所有 {@link IClientModule} 实现类
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class ClientModuleManager {

    @Autowired(required = false)
    private List<IClientModule> modules;

    @Getter
    private List<IClientModule> sortedModules;

    @PostConstruct
    public void init() {
        if (modules == null || modules.isEmpty()) {
            sortedModules = List.of();
            LoggerUtil.info("未发现客户端业务模块");
            return;
        }

        sortedModules = modules.stream()
                .sorted(Comparator.comparingInt(IClientModule::order))
                .toList();

        LoggerUtil.info("已注册 {} 个客户端业务模块: {}",
                sortedModules.size(),
                sortedModules.stream().map(IClientModule::moduleName).toList());
    }
}
