package com.slg.entity.mongo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.entity.mongo.repository.BaseMongoRepository;
import com.slg.entity.mongo.util.MongoConnectionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * MongoDB 数据库配置类
 * 通过 @EnableMongo 注解自动引入
 *
 * <p>配置内容：
 * <ul>
 *   <li>MongoDB 序列化使用 JsonUtil 的 ObjectMapper</li>
 *   <li>启用 MongoDB 审计功能（@CreatedDate、@LastModifiedDate）</li>
 *   <li>导入 MongoDB 连接验证器和仓储实现</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@EnableMongoAuditing
@Import({
    MongoConnectionValidator.class,
    BaseMongoRepository.class
})
public class MongoDbConfiguration {

    /**
     * 配置 MongoDB 转换器
     * 移除默认的 _class 字段，让 Spring Data MongoDB 根据字段类型自动处理序列化
     *
     * @param factory MongoDB 数据库工厂
     * @param context MongoDB 映射上下文
     * @return MappingMongoConverter 实例
     */
    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory factory,
            MongoMappingContext context) {

        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);

        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.afterPropertiesSet();

        return converter;
    }

    /**
     * 配置 Jackson ObjectMapper
     * 使用 @Primary 确保整个应用统一使用 JsonUtil 的配置
     * Spring Data MongoDB 会自动使用这个 ObjectMapper 进行序列化
     *
     * @return JsonUtil 的 ObjectMapper 实例
     */
    @Bean
    @Primary
    public ObjectMapper mongoObjectMapper() {
        ObjectMapper mapper = JsonUtil.getMapper();
        LoggerUtil.debug("MongoDB 使用 JsonUtil 的 ObjectMapper 进行序列化");
        return mapper;
    }
}
