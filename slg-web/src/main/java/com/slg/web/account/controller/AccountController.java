package com.slg.web.account.controller;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.net.zookeeper.model.GameServerZkInfo;
import com.slg.web.account.entity.AccountBindEntity;
import com.slg.web.account.entity.AccountEntity;
import com.slg.web.account.entity.UserEntity;
import com.slg.web.account.model.LoginTokenInfo;
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
 * @author yangxunan
 * @date 2026-02-25
 */
@RestController
@RequestMapping("/api/v1")
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

        // 2. 查找或创建账号
        AccountBindEntity accountBind = accountBindService.findByPlatformIdAndPlatform(platformUid, platform);
        AccountEntity account;
        UserEntity user;
        GameServerZkInfo serverInfo;
        boolean register = false;

        if (accountBind != null) {
            // 老用户：查找 Account 和 User
            account = accountService.findById(accountBind.getAccId());
            if (account == null) {
                LoggerUtil.error("[Login] platformUid={} 关联的 Account={} 不存在", platformUid, accountBind.getAccId());
                return Response.error(ErrorCode.ACCOUNT_NOT_FOUND);
            }

            user = userService.findByRoleId(account.getMainRoleId());
            if (user == null) {
                LoggerUtil.error("[Login] Account={} 的 mainRoleId={} 对应角色不存在", account.getId(), account.getMainRoleId());
                return Response.error(ErrorCode.USER_NOT_FOUND);
            }

            serverInfo = serverService.getById((int) user.getServerId());
            if (serverInfo == null) {
                LoggerUtil.error("[Login] 角色 roleId={} 所在 serverId={} 未找到", user.getRoleId(), user.getServerId());
                return Response.error(ErrorCode.SERVER_NOT_AVAILABLE);
            }
        } else {
            // 新用户：分配服务器 + 创建 Account / AccountBind / User
            serverInfo = getServerForNewUser(serverId, country, bundleId);
            if (serverInfo == null) {
                return Response.error(ErrorCode.RECOMMEND_SERVER_NOT_FOUND);
            }

            account = accountService.createAccount(gaid);
            accountBindService.createAccountBind(platformUid, platform, account.getId());
            user = userService.createUser(serverInfo.getServerId(), account.getId());

            account.setMainRoleId(user.getId());
            account.setLastDeviceId(deviceid);
            account.setIp(ip);
            accountService.save(account);

            register = true;
            LoggerUtil.debug("[Login] 新用户注册, platformUid={}, platform={}, serverId={}", platformUid, platform, serverInfo.getServerId());
        }

        // 3. 封号检查
        if (user.getLockStatus() == UserEntity.LOCK_STATUS_LOCKED) {
            if (user.getLockEndTime() > System.currentTimeMillis()) {
                LoggerUtil.debug("[Login] 账号被封禁, roleId={}", user.getRoleId());
                return Response.error(ErrorCode.ACCOUNT_BANNED);
            }
            user.setLockStatus(UserEntity.LOCK_STATUS_NORMAL);
            user.setLockEndTime(0L);
            userService.save(user);
        }

        // 4. 更新登录信息
        if (!register) {
            user.setLastLoginTime(LocalDateTime.now());
            userService.save(user);
        }
        updateAccountLoginInfo(account, deviceid, platform, bundleId, appversion, country, channel, ip);

        // 5. 生成 loginToken 并缓存到 Redis
        String loginToken = generateAndCacheLoginToken(account, user, serverInfo, platform, register);

        // 6. 构建返回数据
        Map<String, Object> result = buildLoginResult(account, user, serverInfo, authContext, loginToken, register);
        result.put("existRoleServerIds", getExistRoleServerIds(account.getId()));

        LoggerUtil.debug("[Login] 登录成功, platformUid={}, platform={}, serverId={}, register={}",
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
    private String generateAndCacheLoginToken(AccountEntity account, UserEntity user,
                                              GameServerZkInfo serverInfo, int platform, boolean register) {
        String tokenValue = TokenUtils.generateToken();
        String key = LOGIN_TOKEN_PREFIX + tokenValue;

        LoginTokenInfo tokenInfo = LoginTokenInfo.of(
                account.getId(), user.getRoleId(), serverInfo.getServerId(), platform, register);

        int expireMinutes = register ? TOKEN_EXPIRE_MINUTES_REGISTER : TOKEN_EXPIRE_MINUTES;
        redisTemplate.opsForValue().set(key, JsonUtil.toJson(tokenInfo), expireMinutes, TimeUnit.MINUTES);

        return tokenValue;
    }

    /**
     * 构建登录响应数据
     */
    private Map<String, Object> buildLoginResult(AccountEntity account, UserEntity user,
                                                  GameServerZkInfo serverInfo, IAuthContext authContext,
                                                  String loginToken, boolean register) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", account.getId());
        result.put("roleId", user.getRoleId());
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
     * 获取账号已有角色的服务器 ID 列表
     */
    private List<Long> getExistRoleServerIds(long accountId) {
        return userService.findByAccId(accountId).stream()
                .map(UserEntity::getServerId)
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
