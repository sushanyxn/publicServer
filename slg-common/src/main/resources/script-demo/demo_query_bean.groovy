/**
 * 示例：查询 Spring 容器中的 Bean 信息
 *
 * 预置变量:
 *   ctx - Spring ApplicationContext
 *   log - SLF4J Logger
 *   out - PrintWriter (println 输出会被捕获)
 */

def names = ctx.getBeanDefinitionNames()
out.println "已注册 Bean 总数: ${names.length}"
out.println ""
out.println "前 20 个 Bean:"
names.sort().take(20).each { out.println "  - $it" }

return "Total beans: ${names.length}"
