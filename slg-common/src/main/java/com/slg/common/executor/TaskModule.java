package com.slg.common.executor;

/**
 * 任务模块枚举
 * 定义 {@link KeyedVirtualExecutor} 中所有可用的模块类型
 *
 * <p>每个模块对应一类业务场景的任务调度：
 * <ul>
 *   <li>多链模块（如 {@link #PLAYER}）：按 ID 分链，同 ID 串行、不同 ID 并发</li>
 *   <li>单链模块（如 {@link #SYSTEM}）：该模块所有任务共用一条串行链</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/07
 */
public enum TaskModule {

    /**
     * 玩家模块（多链，按 playerId 分链）
     */
    PLAYER("Player", true),

    /**
     * 场景Node（多链，按 NodeId 分链）
     */
    SCENE_NODE("SceneNode", true),

    /**
     * 系统模块（单链）
     */
    SYSTEM("System", false),

     /**
     * 登录模块（单链）
     */
    LOGIN("Login", false),

    /**
     * 场景模块（单链）
     */
    SCENE("Scene", false),

    /**
     * 持久化模块（多链，按实体 ID 分链）
     */
    PERSISTENCE("Persistence", true),

    /**
     * 机器人模块（多链，按机器人 ID 分链）
     */
    ROBOT("Robot", true),

    /**
     * RPC 响应模块（单链）
     * 所有 RPC 响应回调和超时处理在此链中串行执行，避免在 Netty IO 线程中处理
     */
    RPC_RESPONSE("RpcResponse", false);

    /**
     * 模块名称，用于日志展示
     */
    private final String name;

    /**
     * 是否为多链模块
     * true：按 ID 分链（如 PLAYER 按 playerId、PERSISTENCE 按实体 ID）
     * false：该模块所有任务共用一条串行链（如 SYSTEM、LOGIN、SCENE）
     */
    private final boolean multiChain;

    /**
     * 预缓存的单链 TaskKey（单链模块复用同一实例，多链模块此字段为 null）
     * 避免每次 execute/inThread 调用时重复创建短命 TaskKey 对象
     */
    private TaskKey cachedSingleKey;

    TaskModule(String name, boolean multiChain) {
        this.name = name;
        this.multiChain = multiChain;
    }

    // 枚举构造器执行时 TaskKey 类可能尚未完成初始化，使用 static 块延迟缓存
    static {
        for (TaskModule m : values()) {
            if (!m.multiChain) {
                m.cachedSingleKey = new TaskKey(m, 0L);
            }
        }
    }

    /**
     * 获取单链 TaskKey（缓存实例，零分配）
     * 单链模块返回预缓存实例，多链模块返回新建的 id=0 的 TaskKey
     *
     * @return 单链 TaskKey
     */
    public TaskKey toKey() {
        return cachedSingleKey != null ? cachedSingleKey : new TaskKey(this, 0L);
    }

    /**
     * 获取多链 TaskKey
     * 多链模块按 id 创建新 TaskKey，单链模块忽略 id 返回缓存实例
     *
     * @param id 分链标识
     * @return TaskKey
     */
    public TaskKey toKey(long id) {
        return multiChain ? new TaskKey(this, id) : toKey();
    }

    /**
     * 获取模块名称
     *
     * @return 模块名称
     */
    public String getName() {
        return name;
    }

    /**
     * 是否为多链模块
     *
     * @return true 表示按 ID 分链，false 表示单链
     */
    public boolean isMultiChain() {
        return multiChain;
    }

    @Override
    public String toString() {
        return name;
    }
}
