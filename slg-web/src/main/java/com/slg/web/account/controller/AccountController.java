package com.slg.web.account.controller;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.net.zookeeper.model.GameServerZkInfo;
import com.slg.web.account.entity.AccountBindEntity;
import com.slg.web.account.entity.AccountEntity;
import com.slg.web.account.entity.UserEntity;
import com.slg.web.account.model.LoginTokenInfo;
import com.slg.web.account.model.RoleBriefInfo;
import com.slg.web.account.service.AccountBindService;
import com.slg.web.account.service.AccountService;
import com.slg.web.account.service.UserService;
import com.slg.web.auth.AuthRepository;
import com.slg.web.auth.IAuthContext;
import com.slg.web.auth.IAuthService;
import com.slg.web.response.ErrorCode;
import com.slg.web.response.Response;
import com.slg.web.server.service.ServerService;
import com.slg.web.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 客户端登录接口
 * 处理客户端 HTTP 登录请求，完成平台认证、账号管理、服务器分配和 loginToken 生成
 *
 * <p>查询优化说明：
 * <ul>
 *   <li>AccountBind 主键 = platform_platformId，按 (platform, platformId) 查绑定为 O(1)</li>
 *   <li>UserEntity 主键 = roleId，按 roleId 查角色为 O(1)</li>
 *   <li>existRoleServerIds 直接从 Account.roleInfoList 读取，无需查 UserEntity</li>
 *   <li>新用户不在 Web 侧创建 User，由 game 侧创角后通过 RPC 回调写入</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@RestController
@RequestMapping("/api/sanguo2/v1")
public class AccountController {

    private static final String LOGIN_TOKEN_PREFIX = "login:token:";
    /** 普通登录 Token 有效期（分钟） */
    private static final int TOKEN_EXPIRE_MINUTES = 5;
    /** 新注册用户 Token 有效期（分钟），给更多时间完成首次加载 */
    private static final int TOKEN_EXPIRE_MINUTES_REGISTER = 15;

    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountBindService accountBindService;
    @Autowired
    private UserService userService;
    @Autowired
    private ServerService serverService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 客户端登录入口
     *
     * @param id         平台用户 ID（部分平台可为空）
     * @param token      平台认证 token
     * @param platform   平台类型 {@link com.slg.common.constant.PlatformType}
     * @param deviceid   设备 ID
     * @param gaid       广告 ID（GAID）
     * @param appversion 客户端版本号
     * @param serverId   指定服务器 ID（-1 表示由服务端推荐）
     * @param request    HTTP 请求
     * @return 登录结果（含 serverId、gameHost、gamePort、loginToken 等）
     */
    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public Response<Map<String, Object>> login(
            @RequestParam(defaultValue = "") String id,
            @RequestParam String token,
            @RequestParam int platform,
            @RequestParam(defaultValue = "") String deviceid,
            @RequestParam(defaultValue = "") String gaid,
            @RequestParam(defaultValue = "") String appversion,
            @RequestParam(defaultValue = "-1") int serverId,
            HttpServletRequest request) {

        // 1. 平台认证
        IAuthService authService = authRepository.getService(platform);
        if (authService == null) {
            return Response.error(ErrorCode.PLATFORM_NOT_SUPPORTED);
        }

        IAuthContext authContext = authService.auth(id, token);
        if (!authContext.isSuccess()) {
            LoggerUtil.error("[Login] 平台认证失败, platform={}, token={}", platform, token);
            return Response.error(ErrorCode.AUTH_FAILED, "Failed user authentication");
        }

        String platformUid = authContext.getUserId();
        String ip = getClientIp(request);
        String bundleId = request.getParameter("bundleid");
        String country = request.getParameter("country");
        String channel = request.getParameter("channel");

        // 2. 查找或创建账号（O(1) 主键查询）
        AccountBindEntity accountBind = accountBindService.findByPlatformAndId(platform, platformUid);
        AccountEntity account;
        GameServerZkInfo serverInfo;
        long currentRoleId;
        boolean register = false;

        if (accountBind != null) {
            // 老用户：通过 Account 主键拿账号，再从 roleInfoList 直接获取角色信息
            account = accountService.findById(accountBind.getAccId());
            if (account == null) {
                LoggerUtil.error("[Login] platformUid={} 关联的 Account={} 不存在", platformUid, accountBind.getAccId());
                return Response.error(ErrorCode.ACCOUNT_NOT_FOUND);
            }

            currentRoleId = account.getMainRoleId();
            if (currentRoleId == 0 || account.getRoleInfoList().getList().isEmpty()) {
                // 账号存在但尚未创角（游客或新账号游戏内尚未建角色）
                serverInfo = getServerForNewUser(serverId, country, bundleId);
                if (serverInfo == null) {
                    return Response.error(ErrorCode.RECOMMEND_SERVER_NOT_FOUND);
                }
            } else {
                // 从 roleInfoList 直接获取主角色所在服，无需查 UserEntity（O(1)）
                RoleBriefInfo mainRole = accountService.findRoleInfo(account, currentRoleId);
                if (mainRole == null) {
                    LoggerUtil.error("[Login] Account={} mainRoleId={} 不在 roleInfoList 中", account.getId(), currentRoleId);
                    return Response.error(ErrorCode.USER_NOT_FOUND);
                }

                serverInfo = serverService.getById(mainRole.getServerId());
                if (serverInfo == null) {
                    LoggerUtil.error("[Login] roleId={} 所在 serverId={} 未找到", currentRoleId, mainRole.getServerId());
                    return Response.error(ErrorCode.SERVER_NOT_AVAILABLE);
                }

                // 封号检查（按 roleId 主键查，O(1)）
                UserEntity user = userService.findByRoleId(currentRoleId);
                if (user != null && user.getLockStatus() == UserEntity.LOCK_STATUS_LOCKED) {
                    if (user.getLockEndTime() > System.currentTimeMillis()) {
                        LoggerUtil.info("[Login] 账号被封禁, roleId={}", currentRoleId);
                        return Response.error(ErrorCode.ACCOUNT_BANNED);
                    }
                    user.setLockStatus(UserEntity.LOCK_STATUS_NORMAL);
                    user.setLockEndTime(0L);
                    userService.save(user);
                }
            }
        } else {
            // 新用户：分配服务器 + 创建 Account / AccountBind（不在 Web 创建 User）
            serverInfo = getServerForNewUser(serverId, country, bundleId);
            if (serverInfo == null) {
                return Response.error(ErrorCode.RECOMMEND_SERVER_NOT_FOUND);
            }

            account = accountService.createAccount(gaid);
            account.setLastDeviceId(deviceid);
            account.setIp(ip);
            accountService.save(account);

            accountBindService.createAccountBind(platform, platformUid, account.getId());

            currentRoleId = 0;
            register = true;
            LoggerUtil.info("[Login] 新用户注册: platformUid={}, platform={}, serverId={}",
                    platformUid, platform, serverInfo.getServerId());
        }

        // 3. 更新账号登录附加信息
        if (!register) {
            updateAccountLoginInfo(account, deviceid, platform, bundleId, appversion, country, channel, ip);
        }

        // 4. 生成 loginToken 并缓存到 Redis
        String loginToken = generateAndCacheLoginToken(account, currentRoleId, serverInfo, platform, register);

        // 5. 构建返回数据（existRoleServerIds 直接从 Account.roleInfoList 读取，无需查 UserEntity）
        Map<String, Object> result = buildLoginResult(account, currentRoleId, serverInfo, authContext, loginToken, register);
        result.put("existRoleServerIds", getExistRoleServerIds(account));

        LoggerUtil.info("[Login] 登录成功: platformUid={}, platform={}, serverId={}, register={}",
                platformUid, platform, serverInfo.getServerId(), register);
        return Response.success(result);
    }

    /**
     * 为新用户分配服务器
     */
    private GameServerZkInfo getServerForNewUser(int serverId, String country, String bundleId) {
        if (serverId > 0) {
            GameServerZkInfo info = serverService.getById(serverId);
            if (info != null && info.isAlive() && info.isEnable()) {
                return info;
            }
        }
        GameServerZkInfo recommended = serverService.getBestRecommendServer(country, bundleId);
        if (recommended == null) {
            recommended = serverService.getLastServer();
        }
        if (recommended == null) {
            LoggerUtil.error("[Login] 未找到推荐服务器, country={}, bundleId={}", country, bundleId);
        }
        return recommended;
    }

    /**
     * 生成 loginToken 并存入 Redis
     */
    private String generateAndCacheLoginToken(AccountEntity account, long roleId,
                                              GameServerZkInfo serverInfo, int platform, boolean register) {
        String tokenValue = TokenUtils.generateToken();
        String key = LOGIN_TOKEN_PREFIX + tokenValue;

        LoginTokenInfo tokenInfo = LoginTokenInfo.of(
                account.getId(), roleId, serverInfo.getServerId(), platform, register);

        int expireMinutes = register ? TOKEN_EXPIRE_MINUTES_REGISTER : TOKEN_EXPIRE_MINUTES;
        redisTemplate.opsForValue().set(key, JsonUtil.toJson(tokenInfo), expireMinutes, TimeUnit.MINUTES);

        return tokenValue;
    }

    /**
     * 构建登录响应数据
     */
    private Map<String, Object> buildLoginResult(AccountEntity account, long roleId,
                                                  GameServerZkInfo serverInfo, IAuthContext authContext,
                                                  String loginToken, boolean register) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", account.getId());
        result.put("roleId", roleId);
        result.put("serverId", serverInfo.getServerId());
        result.put("serverHost", serverInfo.getGameHost());
        result.put("serverIp", serverInfo.getGameIp());
        result.put("serverPort", serverInfo.getGamePort());
        result.put("register", register);
        result.put("loginToken", loginToken);
        result.put("token", authContext.getUserToken());
        return result;
    }

    /**
     * 获取账号已有角色的服务器 ID 列表（直接从 Account.roleInfoList 读取，无需查 UserEntity）
     */
    private List<Integer> getExistRoleServerIds(AccountEntity account) {
        return account.getRoleInfoList().getList().stream()
                .map(RoleBriefInfo::getServerId)
                .distinct()
                .toList();
    }

    /**
     * 更新账号登录附加信息
     */
    private void updateAccountLoginInfo(AccountEntity account, String deviceId, int platform,
                                         String bundleId, String appVersion, String country,
                                         String channel, String ip) {
        account.setLastLoginTime(LocalDateTime.now());
        account.setCurrPlatform(platform);
        if (deviceId != null && !deviceId.isEmpty()) {
            account.setLastDeviceId(deviceId);
        }
        if (bundleId != null && !bundleId.isEmpty()) {
            account.setAppKey(bundleId);
        }
        if (appVersion != null && !appVersion.isEmpty()) {
            account.setAppVersion(appVersion);
        }
        if (country != null && !country.isEmpty()) {
            account.setCountry(country);
        }
        if (channel != null && !channel.isEmpty()) {
            account.setChannel(channel);
        }
        if (ip != null && !ip.isEmpty()) {
            account.setIp(ip);
        }
        accountService.save(account);
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
