package com.slg.client.core.account;

import com.slg.net.socket.model.NetSession;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端账号上下文
 * 每个登录的账号拥有独立的 WebSocket 连接、数据状态和 UI 上下文
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
public class ClientAccount {

    private final String account;

    private long playerId;

    private NetSession session;

    private boolean loggedIn;

    /**
     * JavaFX 可观察属性，用于 UI 绑定
     */
    private final SimpleStringProperty accountProperty;
    private final SimpleLongProperty playerIdProperty;
    private final SimpleBooleanProperty loggedInProperty;

    /**
     * 各业务模块的数据存储，key 为模块名
     */
    private final Map<String, Object> moduleDataMap = new ConcurrentHashMap<>();

    public ClientAccount(String account) {
        this.account = account;
        this.accountProperty = new SimpleStringProperty(account);
        this.playerIdProperty = new SimpleLongProperty(0);
        this.loggedInProperty = new SimpleBooleanProperty(false);
    }

    /**
     * 发送消息到服务器
     */
    public void sendMessage(Object msg) {
        if (session != null && session.isActive()) {
            session.sendMessage(msg);
        }
    }

    /**
     * 绑定网络会话
     */
    public void bindSession(NetSession session) {
        this.session = session;
    }

    /**
     * 标记登录成功
     */
    public void onLoginSuccess(long playerId) {
        this.playerId = playerId;
        this.loggedIn = true;
        this.playerIdProperty.set(playerId);
        this.loggedInProperty.set(true);
    }

    /**
     * 获取模块数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getModuleData(String moduleName, Class<T> type) {
        return (T) moduleDataMap.get(moduleName);
    }

    /**
     * 设置模块数据
     */
    public void setModuleData(String moduleName, Object data) {
        moduleDataMap.put(moduleName, data);
    }

    /**
     * 是否连接中
     */
    public boolean isConnected() {
        return session != null && session.isActive();
    }

    @Override
    public String toString() {
        return loggedIn
                ? account + " [" + playerId + "]"
                : account + " [未登录]";
    }
}
