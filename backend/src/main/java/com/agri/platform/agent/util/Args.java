package com.agri.platform.agent.util;

/**
 * LLM 工具参数健壮转换。模型可能把整数参数返回成 "1024.0"(hutool 解析为 Double/BigDecimal),
 * 直接 Integer.valueOf 会抛 NumberFormatException 且报错信息不可读,统一在此兜底。
 */
public final class Args {

    private Args() {}

    /** 转 Integer:兼容 Number 实例、整数字符串、浮点字符串(截断取整);失败抛带参数值的业务异常。 */
    public static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        String s = o.toString().trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("参数格式错误(应为数字): " + s);
            }
        }
    }

    /** 转 BigDecimal:兼容 Number 与字符串;失败抛带参数值的业务异常。 */
    public static java.math.BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof java.math.BigDecimal d) return d;
        try {
            return new java.math.BigDecimal(o.toString().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("参数格式错误(应为数字): " + o);
        }
    }

    /** 转 String(null 安全);显式传 JSON null 时返回 null 而非 NPE。 */
    public static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }
}
