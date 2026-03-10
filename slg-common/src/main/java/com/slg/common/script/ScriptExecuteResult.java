package com.slg.common.script;

import lombok.Getter;

/**
 * Groovy 脚本执行结果
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
public class ScriptExecuteResult {

    private final boolean success;
    private final Object result;
    private final String output;
    private final String errorMessage;
    private final long costMs;

    private ScriptExecuteResult(boolean success, Object result, String output, String errorMessage, long costMs) {
        this.success = success;
        this.result = result;
        this.output = output;
        this.errorMessage = errorMessage;
        this.costMs = costMs;
    }

    public static ScriptExecuteResult valueOf(Object result, String output, long costMs) {
        return new ScriptExecuteResult(true, result, output, null, costMs);
    }

    public static ScriptExecuteResult error(String errorMessage, String output, long costMs) {
        return new ScriptExecuteResult(false, null, output, errorMessage, costMs);
    }

    /**
     * 获取返回值的字符串表示
     */
    public String getResultString() {
        return result != null ? result.toString() : "null";
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (success) {
            sb.append(String.format("脚本执行成功, 耗时 %dms", costMs));
            if (result != null) {
                sb.append(", 返回值: ").append(getResultString());
            }
        } else {
            sb.append(String.format("脚本执行失败, 耗时 %dms, 错误: %s", costMs, errorMessage));
        }
        if (output != null && !output.isEmpty()) {
            sb.append("\n--- 输出 ---\n").append(output);
        }
        return sb.toString();
    }
}
