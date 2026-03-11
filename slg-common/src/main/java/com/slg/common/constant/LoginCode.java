package com.slg.common.constant;

/**
 * 登录响应码
 *
 * @author yangxunan
 * @date 2026/1/22
 */
public interface LoginCode {

    int SUCCESS = 0;
    /** 账号为空 */
    int FAIL_ACCOUNT_EMPTY = 1;
    /** 指定角色不属于该账号 */
    int FAIL_ROLE_NOT_IN_ACCOUNT = 2;

}
