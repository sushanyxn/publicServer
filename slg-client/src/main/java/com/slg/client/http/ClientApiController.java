package com.slg.client.http;

import com.slg.client.core.ClientException;
import com.slg.client.core.account.AccountManager;
import com.slg.client.core.account.ClientAccount;
import com.slg.client.message.hero.HeroClientHandler;
import com.slg.net.message.clientmessage.gm.packet.CM_GMCommand;
import com.slg.net.message.clientmessage.hero.packet.CM_HeroLevelUp;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端 HTTP API
 * 供 AI 自动化测试和外部工具调用
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@RestController
@RequestMapping("/api")
public class ClientApiController {

    @Autowired
    private AccountManager accountManager;

    /**
     * 登录指定账号
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> req) {
        String account = (String) req.get("account");
        long playerId = req.containsKey("playerId")
                ? ((Number) req.get("playerId")).longValue()
                : 0;

        try {
            accountManager.login(account, playerId);
        } catch (ClientException e) {
            return Map.of("success", false, "message", e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("account", account);
        result.put("message", "登录请求已发送");
        return result;
    }

    /**
     * 获取所有在线账号
     */
    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts() {
        return accountManager.getAllAccounts().stream()
                .map(this::toAccountInfo)
                .toList();
    }

    /**
     * 获取指定账号的信息
     */
    @GetMapping("/account/{name}")
    public Map<String, Object> accountInfo(@PathVariable String name) {
        ClientAccount account = accountManager.getByAccountName(name);
        if (account == null) {
            return Map.of("success", false, "message", "账号不存在");
        }
        return toAccountInfo(account);
    }

    /**
     * 断开指定账号
     */
    @PostMapping("/account/{name}/disconnect")
    public Map<String, Object> disconnect(@PathVariable String name) {
        accountManager.disconnect(name);
        return Map.of("success", true, "message", "已断开连接");
    }

    /**
     * 获取指定账号的英雄列表
     */
    @GetMapping("/account/{name}/heroes")
    public Map<String, Object> heroList(@PathVariable String name) {
        ClientAccount account = accountManager.getByAccountName(name);
        if (account == null) {
            return Map.of("success", false, "message", "账号不存在");
        }

        @SuppressWarnings("unchecked")
        ObservableList<HeroVO> heroList = account.getModuleData(HeroClientHandler.MODULE_KEY, ObservableList.class);
        if (heroList == null || heroList.isEmpty()) {
            return Map.of("success", true, "heroes", List.of());
        }

        List<Map<String, Object>> heroes = heroList.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("heroId", h.getHeroId());
            m.put("heroLv", h.getHeroLv());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("heroes", heroes);
        return result;
    }

    /**
     * 请求英雄升级
     */
    @PostMapping("/account/{name}/hero/levelup")
    public Map<String, Object> heroLevelUp(@PathVariable String name, @RequestBody Map<String, Object> req) {
        ClientAccount account = accountManager.getByAccountName(name);
        if (account == null) {
            return Map.of("success", false, "message", "账号不存在");
        }
        if (!account.isLoggedIn()) {
            return Map.of("success", false, "message", "账号未登录");
        }

        int heroId = ((Number) req.get("heroId")).intValue();
        CM_HeroLevelUp packet = new CM_HeroLevelUp();
        packet.setHeroId(heroId);
        account.sendMessage(packet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "英雄升级请求已发送");
        result.put("heroId", heroId);
        return result;
    }

    /**
     * 发送 GM 指令
     */
    @PostMapping("/account/{name}/gm")
    public Map<String, Object> gmCommand(@PathVariable String name, @RequestBody Map<String, Object> req) {
        ClientAccount account = accountManager.getByAccountName(name);
        if (account == null) {
            return Map.of("success", false, "message", "账号不存在");
        }
        if (!account.isLoggedIn()) {
            return Map.of("success", false, "message", "账号未登录");
        }

        String command = (String) req.get("command");
        if (command == null || command.isBlank()) {
            return Map.of("success", false, "message", "指令不能为空");
        }

        CM_GMCommand packet = new CM_GMCommand();
        packet.setCommand(command);
        account.sendMessage(packet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "GM 指令已发送");
        result.put("command", command);
        return result;
    }

    private Map<String, Object> toAccountInfo(ClientAccount account) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("account", account.getAccount());
        info.put("playerId", account.getPlayerId());
        info.put("loggedIn", account.isLoggedIn());
        info.put("connected", account.isConnected());
        return info;
    }
}
