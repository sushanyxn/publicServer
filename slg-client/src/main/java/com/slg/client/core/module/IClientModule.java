package com.slg.client.core.module;

import com.slg.client.core.account.ClientAccount;
import javafx.scene.layout.Pane;

/**
 * 客户端业务模块接口
 * 每个业务模块（英雄、城建、联盟等）实现此接口以接入模块化框架
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public interface IClientModule {

    /**
     * 模块名称（用于导航菜单显示）
     */
    String moduleName();

    /**
     * 模块排序权重（越小越靠前）
     */
    default int order() {
        return 100;
    }

    /**
     * 为指定账号创建模块 UI 面板
     *
     * @param account 当前账号上下文
     * @return JavaFX 面板
     */
    Pane createPanel(ClientAccount account);
}
