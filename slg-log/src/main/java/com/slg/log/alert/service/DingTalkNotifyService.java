package com.slg.log.alert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 钉钉 Webhook 推送服务
 * 通过钉钉机器人 Webhook 发送告警通知
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
public class DingTalkNotifyService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送告警通知到钉钉
     *
     * @param webhookUrl      钉钉 Webhook 地址
     * @param ruleName        规则名称
     * @param matchCount      匹配数量
     * @param level           日志级别
     * @param keyword         关键词
     * @param serverId        服务器ID
     * @param timeWindowMinutes 时间窗口
     * @return 通知结果描述，成功返回 "SUCCESS"，失败返回错误信息
     */
    public String sendAlert(String webhookUrl, String ruleName, long matchCount,
                            String level, String keyword, String serverId, int timeWindowMinutes) {
        try {
            String text = buildMarkdown(ruleName, matchCount, level, keyword, serverId, timeWindowMinutes);

            Map<String, Object> body = Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of(
                            "title", "SLG 日志告警",
                            "text", text
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(webhookUrl, request, String.class);
            log.debug("[DingTalk] 告警推送成功, rule={}, response={}", ruleName, response);
            return "SUCCESS";
        } catch (Exception e) {
            log.error("[DingTalk] 告警推送失败, rule={}, url={}", ruleName, webhookUrl, e);
            return e.getMessage();
        }
    }

    /**
     * 发送测试消息到钉钉
     *
     * @param webhookUrl 钉钉 Webhook 地址
     * @return 成功返回 "SUCCESS"，失败返回错误信息
     */
    public String sendTestMessage(String webhookUrl) {
        try {
            String text = "## SLG 日志告警 - 测试消息\n\n"
                    + "- **状态**: 连接测试成功\n"
                    + "- **时间**: " + LocalDateTime.now().format(FMT) + "\n\n"
                    + "> 此消息为 Webhook 连通性测试，无需处理。";

            Map<String, Object> body = Map.of(
                    "msgtype", "markdown",
                    "markdown", Map.of(
                            "title", "SLG 告警测试",
                            "text", text
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(webhookUrl, request, String.class);
            return "SUCCESS";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String buildMarkdown(String ruleName, long matchCount, String level,
                                 String keyword, String serverId, int timeWindowMinutes) {
        StringBuilder sb = new StringBuilder();
        sb.append("## SLG 日志告警\n\n");
        sb.append("- **规则**: ").append(ruleName).append("\n");
        sb.append("- **匹配数**: ").append(matchCount).append(" 条\n");
        sb.append("- **时间窗口**: 最近 ").append(timeWindowMinutes).append(" 分钟\n");
        if (level != null && !level.isEmpty()) {
            sb.append("- **日志级别**: ").append(level).append("\n");
        }
        if (keyword != null && !keyword.isEmpty()) {
            sb.append("- **关键词**: ").append(keyword).append("\n");
        }
        if (serverId != null && !serverId.isEmpty()) {
            sb.append("- **服务器**: ").append(serverId).append("\n");
        } else {
            sb.append("- **服务器**: 全部\n");
        }
        sb.append("- **触发时间**: ").append(LocalDateTime.now().format(FMT)).append("\n");
        return sb.toString();
    }
}
