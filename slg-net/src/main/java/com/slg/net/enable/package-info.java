/**
 * 各“启用”注解（如 @EnableRpcRoute）通过 @Import 引入的标记配置类所在包。
 * 本包不在各进程的 ComponentScan 范围内，仅在使用对应 @Enable* 注解时才会加载，
 * 便于放置更多类似的“按需启用”配置。
 */
package com.slg.net.enable;
