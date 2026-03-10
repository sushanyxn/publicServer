/**
 * 示例：通过 Groovy 脚本触发热更
 *
 * 预置变量:
 *   ctx - Spring ApplicationContext
 *   log - SLF4J Logger
 *   out - PrintWriter (println 输出会被捕获)
 *
 * 使用前请将 /path/to/hot-classes 替换为实际的 class 文件目录
 */

def manager = ctx.getBean(com.slg.common.hotreload.HotReloadManager)
def result = manager.reload("/path/to/hot-classes")
out.println result.summary

return result.allSuccess ? "热更全部成功" : "热更存在失败项，请查看详情"
