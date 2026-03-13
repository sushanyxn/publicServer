package com.slg.client.core.account;

import com.slg.client.core.ClientException;
import com.slg.client.core.config.ClientConfig;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.net.socket.model.NetSession;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多账号管理器
 * 管理所有客户端账号的创建、登录、切换和销毁
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class AccountManager {

    @Autowired
    private ClientConfig clientConfig;

    /**
     * 通过 session 映射到账号（用于消息路由）
     */
    private final Map<NetSession, ClientAccount> sessionAccountMap = new ConcurrentHashMap<>();

    /**
     * 通过 account 名映射到账号
     */
    private final Map<String, ClientAccount> accountMap = new ConcurrentHashMap<>();

    /**
     * 当前 UI 聚焦的账号
     */
    @Getter
    private volatile ClientAccount activeAccount;

    /**
     * 创建账号并登录
     *
     * @param account  账号名
     * @param playerId 玩家 ID（0 表示新登录）
     * @return 创建的账号上下文
     * @throws ClientException 账号已在线时抛出
     */
    public ClientAccount login(String account, long playerId) {
        ClientAccount existing = accountMap.get(account);
        if (existing != null && existing.isConnected()) {
            throw new ClientException("账号 " + account + " 已在线，请勿重复登录");
        }

        ClientAccount clientAccount = new ClientAccount(account);
        accountMap.put(account, clientAccount);

        NetSession session = WebSocketClientManager.getInstance().connect(clientConfig.getServerUrl());
        clientAccount.bindSession(session);
        sessionAccountMap.put(session, clientAccount);

        CM_LoginReq loginReq = new CM_LoginReq();
        loginReq.setAccount(account);
        loginReq.setPlayerId(playerId);
        clientAccount.sendMessage(loginReq);

        LoggerUtil.info("账号 {} 正在连接服务器 {}", account, clientConfig.getServerUrl());

        if (activeAccount == null) {
            activeAccount = clientAccount;
        }

        return clientAccount;
    }

    /**
     * 通过 session 获取账号
     */
    public ClientAccount getBySession(NetSession session) {
        return sessionAccountMap.get(session);
    }

    /**
     * 通过账号名获取账号
     */
    public ClientAccount getByAccountName(String account) {
        return accountMap.get(account);
    }

    /**
     * 切换当前活跃账号
     */
    public void switchTo(ClientAccount account) {
        this.activeAccount = account;
    }

    /**
     * 获取所有账号
     */
    public Collection<ClientAccount> getAllAccounts() {
        return accountMap.values();
    }

    /**
     * 断开指定账号
     */
    public void disconnect(String accountName) {
        ClientAccount account = accountMap.remove(accountName);
        if (account != null) {
            sessionAccountMap.remove(account.getSession());
            if (account.getSession() != null) {
                account.getSession().close("主动断开");
            }
            if (activeAccount == account) {
                activeAccount = accountMap.values().stream().findFirst().orElse(null);
            }
            LoggerUtil.info("账号 {} 已断开", accountName);
        }
    }

    /**
     * 重连指定账号（重新建立 WebSocket 并发送登录请求）
     */
    public void reconnect(ClientAccount account) {
        NetSession oldSession = account.getSession();
        if (oldSession != null) {
            sessionAccountMap.remove(oldSession);
        }

        NetSession session = WebSocketClientManager.getInstance().connect(clientConfig.getServerUrl());
        account.bindSession(session);
        sessionAccountMap.put(session, account);

        CM_LoginReq loginReq = new CM_LoginReq();
        loginReq.setAccount(account.getAccount());
        loginReq.setPlayerId(account.getPlayerId());
        account.sendMessage(loginReq);

        LoggerUtil.info("账号 {} 正在重连服务器 {}", account.getAccount(), clientConfig.getServerUrl());
    }

    /**
     * 移除 session 映射（连接断开时调用）
     */
    public void onSessionClosed(NetSession session) {
        ClientAccount account = sessionAccountMap.remove(session);
        if (account != null) {
            account.setLoggedIn(false);
            account.getLoggedInProperty().set(false);
            LoggerUtil.info("账号 {} 连接已断开", account.getAccount());
        }
    }
}
