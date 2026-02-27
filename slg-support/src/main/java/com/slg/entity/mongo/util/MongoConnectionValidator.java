package com.slg.entity.mongo.util;

import com.slg.common.log.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 连接验证工具
 * 用于测试和验证 MongoDB 连接状态
 * 通过 @EnableMongo 自动引入，不参与组件扫描
 * 
 * @author yangxunan
 * @date 2025-12-23
 */
public class MongoConnectionValidator {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 验证 MongoDB 连接
     * 执行一个简单的操作来触发真正的连接建立
     * 
     * @return true 如果连接成功
     */
    public boolean validateConnection() {
        try {
            LoggerUtil.debug("开始验证 MongoDB 连接...");
            
            // 获取数据库名称（这会触发真正的连接）
            String dbName = mongoTemplate.getDb().getName();
            
            // 执行 ping 命令验证连接
            org.bson.Document result = mongoTemplate.getDb()
                .runCommand(new org.bson.Document("ping", 1));
            
            LoggerUtil.debug("MongoDB 连接验证成功！");
            LoggerUtil.debug("  数据库名称: {}", dbName);
            LoggerUtil.debug("  服务器响应: {}", result.get("ok"));
            
            return true;
            
        } catch (Exception e) {
            LoggerUtil.error("MongoDB 连接验证失败！", e);
            LoggerUtil.error("  请检查：");
            LoggerUtil.error("    1. MongoDB 服务是否启动？");
            LoggerUtil.error("    2. 连接 URI 是否正确？");
            LoggerUtil.error("    3. 网络是否可达？");
            return false;
        }
    }

    /**
     * 检查 MongoDB 是否可用
     * 不抛出异常，只返回布尔值
     * 
     * @return true 如果 MongoDB 可用
     */
    public boolean isMongoAvailable() {
        try {
            mongoTemplate.getDb().getName();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}



