package com.slg.game.base.account.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.game.SpringContext;
import com.slg.game.base.account.model.AccountRoleBrief;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家账号实体
 * 描述玩家账号（字符串主键）与玩家角色的对应关系，一个账号下可有多个角色，关联各角色的简要信息（如最近登录时间等）
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "player_account")
@CacheConfig(maxSize = -1, expireMinutes = -1, writeDelay = false, skipDbOnMiss = true)
@FieldNameConstants
public class AccountEntity extends BaseEntity<String> {

    /**
     * 该账号下所有角色的简要信息列表（含角色 ID、最近登录时间等，可扩展）
     */
    private List<AccountRoleBrief> roleBriefList = new ArrayList<>();

    @Override
    public void save() {
        SpringContext.getAccountManager().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        SpringContext.getAccountManager().saveField(this, fieldName);
    }
}
