package com.agri.platform.agent.util;

/**
 * LLM 工具参数健壮转换。模型可能把整数参数返回成 "1024.0"(hutool 解析为 Double/BigDecimal),
 * 或带单位/分隔符的数字串("50000元" "50,000" "2斤"),直接 Integer.valueOf/BigDecimal 构造
 * 会抛 NumberFormatException 且报错信息不可读,统一在此兜底。
 */
public final class Args {

    private Args() {}

    /** 转 Integer:兼容 Number 实例、整数字符串、浮点字符串(截断取整)、带单位数字串;
     *  空白/无数字输入返回 null(视为未提供),失败抛业务异常。 */
    public static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        String s = digitsOnly(o);
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("参数格式错误(应为数字): " + o);
            }
        }
    }

    /** 转 BigDecimal:兼容 Number 与数字串(容忍"元"/千分位逗号/单位后缀);
     *  空白/无数字输入返回 null(视为未提供),失败抛业务异常。 */
    public static java.math.BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof java.math.BigDecimal d) return d;
        String s = digitsOnly(o);
        if (s == null) return null;
        try {
            return new java.math.BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException("参数格式错误(应为数字): " + o);
        }
    }

    /** 转 String(null 安全);显式传 JSON null 时返回 null 而非 NPE。 */
    public static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }

    /** 剥掉数字串里的单位/分隔符(只留数字、小数点、负号);空白输入返回 null(视为未提供)。 */
    private static String digitsOnly(Object o) {
        String s = o.toString().trim().replaceAll("[^0-9.\\-]", "");
        return s.isEmpty() ? null : s;
    }
}
