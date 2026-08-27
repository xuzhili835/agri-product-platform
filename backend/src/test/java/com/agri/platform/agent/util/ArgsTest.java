package com.agri.platform.agent.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** LLM 参数健壮转换:模型常传 "50000元"/"50,000"/"2斤"/空串,解析不得抛裸异常。 */
class ArgsTest {

    @Test
    void toBigDecimalToleratesUnitsAndSeparators() {
        assertEquals(new BigDecimal("50000"), Args.toBigDecimal("50000元"));
        assertEquals(new BigDecimal("50000"), Args.toBigDecimal("50,000"));
        assertEquals(new BigDecimal("50000"), Args.toBigDecimal(" 50000 "));
        assertEquals(new BigDecimal("3.5"), Args.toBigDecimal(3.5));
        assertEquals(new BigDecimal("3.5"), Args.toBigDecimal("3.5"));
    }

    @Test
    void toBigDecimalBlankOrNullIsMissing() {
        assertNull(Args.toBigDecimal(null));
        assertNull(Args.toBigDecimal(""));
        assertNull(Args.toBigDecimal("  "));
    }

    @Test
    void toBigDecimalGarbageBecomesMissing() {
        // 纯垃圾串("abc")剥掉单位后无数字 → 视为未提供(null),由上层 validate 给"请填写金额"类友好提示
        assertNull(Args.toBigDecimal("abc"));
        assertNull(Args.toInt("abc"));
    }

    @Test
    void toIntToleratesUnitsAndFloatForms() {
        assertEquals(5, Args.toInt("5亩"));
        assertEquals(2, Args.toInt("2斤"));
        assertEquals(1024, Args.toInt("1024.0"));
        assertEquals(1024, Args.toInt(1024.0));
        assertEquals(7, Args.toInt(7));
    }

    @Test
    void toIntBlankOrNullIsMissing() {
        assertNull(Args.toInt(null));
        assertNull(Args.toInt(""));
    }

    @Test
    void strTrimsAndNullSafe() {
        assertEquals("芒果", Args.str(" 芒果 "));
        assertNull(Args.str(null));
    }
}
