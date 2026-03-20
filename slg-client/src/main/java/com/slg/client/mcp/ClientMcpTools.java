package com.slg.client.mcp;

import com.slg.client.core.ClientException;
import com.slg.client.core.account.AccountManager;
import com.slg.client.core.account.ClientAccount;
import com.slg.client.message.hero.HeroClientHandler;
import com.slg.net.message.clientmessage.gm.packet.CM_GMCommand;
import com.slg.net.message.clientmessage.hero.packet.CM_HeroLevelUp;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import javafx.collections.ObservableList;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户端 MCP 工具
 * 供 Cursor AI 通过 MCP 协议调用，执行客户端模拟器操作
 *
 * @author yangxunan
 * @date 2026/03/20
 */
@Component
public class ClientMcpTools {

    @Autowired
    private AccountManager accountManager;

    @Tool(name = "login", description = "登录指定账号到游戏服务器，建立 WebSocket 连接并发送登录请求")
    public String login(
            @ToolParam(description = "账号名") String account,
            @ToolParam(description = "玩家ID，首次登录传0或不传", required = false) Long playerId) {
        try {
            accountManager.login(account, playerId != null ? playerId : 0);
        } catch (ClientException e) {
            return "登录失败: " + e.getMessage();
        }
        return "账号 " + account + " 登录请求已发送";
    }

    @Tool(name = "list_accounts", description = "获取所有已登录的账号列表，包含账号名、玩家ID、登录状态、连接状态")
    public String listAccounts() {
        Collection<ClientAccount> accounts = accountManager.getAllAccounts();
        if (accounts.isEmpty()) {
            return "当前没有已登录的账号";
        }
        return accounts.stream()
                .map(this::formatAccountInfo)
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "get_account", description = "获取指定账号的详细信息，包含账号名、玩家ID、登录状态、连接状态")
    public String getAccount(
            @ToolParam(description = "账号名") String account) {
        ClientAccount clientAccount = accountManager.getByAccountName(account);
        if (clientAccount == null) {
            return "账号不存在: " + account;
        }
        return formatAccountInfo(clientAccount);
    }

    @Tool(name = "disconnect", description = "断开指定账号与游戏服务器的连接")
    public String disconnect(
            @ToolParam(description = "账号名") String account) {
        accountManager.disconnect(account);
        return "账号 " + account + " 已断开连接";
    }

    @Tool(name = "list_heroes", description = "获取指定账号的英雄列表，包含英雄ID和等级")
    public String listHeroes(
            @ToolParam(description = "账号名") String account) {
        ClientAccount clientAccount = accountManager.getByAccountName(account);
        if (clientAccount == null) {
            return "账号不存在: " + account;
        }

        @SuppressWarnings("unchecked")
        ObservableList<HeroVO> heroList = clientAccount.getModuleData(HeroClientHandler.MODULE_KEY, ObservableList.class);
        if (heroList == null || heroList.isEmpty()) {
            return "账号 " + account + " 暂无英雄数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("账号 ").append(account).append(" 的英雄列表:\n");
        for (HeroVO hero : heroList) {
            sb.append("  - heroId=").append(hero.getHeroId())
                    .append(", lv=").append(hero.getHeroLv())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    @Tool(name = "hero_levelup", description = "请求指定英雄升级，需要账号已登录")
    public String heroLevelUp(
            @ToolParam(description = "账号名") String account,
            @ToolParam(description = "英雄ID") int heroId) {
        ClientAccount clientAccount = accountManager.getByAccountName(account);
        if (clientAccount == null) {
            return "账号不存在: " + account;
        }
        if (!clientAccount.isLoggedIn()) {
            return "账号 " + account + " 未登录";
        }

        CM_HeroLevelUp packet = new CM_HeroLevelUp();
        packet.setHeroId(heroId);
        clientAccount.sendMessage(packet);
        return "英雄升级请求已发送, heroId=" + heroId;
    }

    @Tool(name = "gm_command", description = "发送 GM 指令到游戏服务器，需要账号已登录")
    public String gmCommand(
            @ToolParam(description = "账号名") String account,
            @ToolParam(description = "GM 指令内容") String command) {
        ClientAccount clientAccount = accountManager.getByAccountName(account);
        if (clientAccount == null) {
            return "账号不存在: " + account;
        }
        if (!clientAccount.isLoggedIn()) {
            return "账号 " + account + " 未登录";
        }
        if (command == null || command.isBlank()) {
            return "GM 指令不能为空";
        }

        CM_GMCommand packet = new CM_GMCommand();
        packet.setCommand(command);
        clientAccount.sendMessage(packet);
        return "GM 指令已发送: " + command;
    }

    private String formatAccountInfo(ClientAccount account) {
        return "account=" + account.getAccount()
                + ", playerId=" + account.getPlayerId()
                + ", loggedIn=" + account.isLoggedIn()
                + ", connected=" + account.isConnected();
    }
}
