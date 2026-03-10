/**
 * 示例：查询当前 JVM 系统信息
 *
 * 预置变量:
 *   ctx - Spring ApplicationContext
 *   log - SLF4J Logger
 *   out - PrintWriter (println 输出会被捕获)
 */

def runtime = Runtime.getRuntime()
def mb = 1024 * 1024

out.println "=== JVM 信息 ==="
out.println "Java 版本: ${System.getProperty('java.version')}"
out.println "VM 名称:   ${System.getProperty('java.vm.name')}"
out.println ""
out.println "=== 内存状态 ==="
out.println "最大内存:   ${runtime.maxMemory() / mb} MB"
out.println "已分配:     ${runtime.totalMemory() / mb} MB"
out.println "空闲:       ${runtime.freeMemory() / mb} MB"
out.println "已使用:     ${(runtime.totalMemory() - runtime.freeMemory()) / mb} MB"
out.println ""
out.println "=== 线程信息 ==="
out.println "活跃线程数: ${Thread.activeCount()}"
out.println ""
out.println "=== Agent 状态 ==="
out.println "Instrumentation 可用: ${com.slg.common.hotreload.InstrumentationHolder.isAvailable()}"

return "系统信息查询完成"
