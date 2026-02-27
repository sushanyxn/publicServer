package com.slg.net.rpc.anno;

import java.lang.annotation.*;

/**
 * RPC 代理对象注入注解
 * 用于在 Spring Bean 中自动注入 RPC 接口的代理对象
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @Component
 * public class GameService {
 *     @RpcRef
 *     private ISceneRpcService sceneService;
 *
 *     public void test() {
 *         sceneService.watch(10001, 123L, 100, 200);
 *     }
 * }
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/01/23
 * @see com.slg.net.rpc.inject.RpcRefInjector
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcRef {
}
