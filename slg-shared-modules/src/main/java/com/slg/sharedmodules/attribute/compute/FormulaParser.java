package com.slg.sharedmodules.attribute.compute;

import com.slg.sharedmodules.attribute.type.AttributeType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 属性公式解析器。
 * <p>
 * 将公式字符串解析为 {@link AttributeFormula}，使用递归下降算法。
 * 公式语法：
 * <ul>
 *   <li>变量：AttributeType 枚举名称（如 BASE_ATK、ATK_PCT）</li>
 *   <li>常量：整数字面量（如 10000）</li>
 *   <li>运算符：+、-、*、/（整数除法，截断）</li>
 *   <li>括号：( )</li>
 *   <li>一元负号：-BASE_ATK</li>
 * </ul>
 * 示例：{@code BASE_ATK * (10000 + ATK_PCT) / 10000 + EXTRA_ATK}
 * </p>
 *
 * @author yangxunan
 * @date 2026-03-12
 */
public class FormulaParser {

    private final String source;
    private int pos;
    private final Set<AttributeType> dependencies = new LinkedHashSet<>();

    private FormulaParser(String source) {
        this.source = source;
        this.pos = 0;
    }

    /**
     * 解析公式字符串，生成属性公式
     *
     * @param target     目标计算属性
     * @param expression 公式表达式
     * @return 属性公式实例
     * @throws IllegalArgumentException 公式语法错误
     */
    public static AttributeFormula parse(AttributeType target, String expression) {
        FormulaParser parser = new FormulaParser(expression.trim());
        AttributeFormula.Calculator calculator = parser.parseExpression();
        parser.skipWhitespace();
        if (parser.pos < parser.source.length()) {
            throw new IllegalArgumentException(
                    "属性 " + target + " 公式解析失败：位置 " + parser.pos + " 处存在多余字符 '"
                            + parser.source.charAt(parser.pos) + "'");
        }
        List<AttributeType> deps = new ArrayList<>(parser.dependencies);
        return AttributeFormula.of(target, deps, calculator);
    }

    /**
     * expression := term (('+' | '-') term)*
     */
    private AttributeFormula.Calculator parseExpression() {
        AttributeFormula.Calculator result = parseTerm();
        skipWhitespace();
        while (pos < source.length() && (peek() == '+' || peek() == '-')) {
            char op = source.charAt(pos++);
            AttributeFormula.Calculator right = parseTerm();
            AttributeFormula.Calculator left = result;
            result = op == '+'
                    ? values -> left.calculate(values) + right.calculate(values)
                    : values -> left.calculate(values) - right.calculate(values);
            skipWhitespace();
        }
        return result;
    }

    /**
     * term := unary (('*' | '/') unary)*
     */
    private AttributeFormula.Calculator parseTerm() {
        AttributeFormula.Calculator result = parseUnary();
        skipWhitespace();
        while (pos < source.length() && (peek() == '*' || peek() == '/')) {
            char op = source.charAt(pos++);
            AttributeFormula.Calculator right = parseUnary();
            AttributeFormula.Calculator left = result;
            result = op == '*'
                    ? values -> left.calculate(values) * right.calculate(values)
                    : values -> {
                        long divisor = right.calculate(values);
                        if (divisor == 0) {
                            return 0L;
                        }
                        return left.calculate(values) / divisor;
                    };
            skipWhitespace();
        }
        return result;
    }

    /**
     * unary := '-' unary | primary
     */
    private AttributeFormula.Calculator parseUnary() {
        skipWhitespace();
        if (pos < source.length() && peek() == '-') {
            pos++;
            AttributeFormula.Calculator operand = parseUnary();
            return values -> -operand.calculate(values);
        }
        return parsePrimary();
    }

    /**
     * primary := NUMBER | IDENTIFIER | '(' expression ')'
     */
    private AttributeFormula.Calculator parsePrimary() {
        skipWhitespace();
        if (pos >= source.length()) {
            throw error("表达式意外结束");
        }

        char c = peek();

        if (c == '(') {
            pos++;
            AttributeFormula.Calculator result = parseExpression();
            skipWhitespace();
            expect(')');
            return result;
        }

        if (Character.isDigit(c)) {
            long value = readNumber();
            return values -> value;
        }

        if (Character.isLetter(c) || c == '_') {
            String name = readIdentifier();
            AttributeType type;
            try {
                type = AttributeType.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw error("未知的属性名称 '" + name + "'");
            }
            dependencies.add(type);
            return values -> values.applyAsLong(type);
        }

        throw error("无法识别的字符 '" + c + "'");
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        return source.charAt(pos);
    }

    private void expect(char expected) {
        if (pos >= source.length() || source.charAt(pos) != expected) {
            throw error("期望 '" + expected + "'");
        }
        pos++;
    }

    private long readNumber() {
        int start = pos;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
            pos++;
        }
        return Long.parseLong(source.substring(start, pos));
    }

    private String readIdentifier() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
            pos++;
        }
        return source.substring(start, pos);
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("公式解析失败（位置 " + pos + "）：" + message + " | 公式：" + source);
    }
}
