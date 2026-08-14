package com.agri.platform.agent.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PiiMaskerTest {
    private final PiiMasker m = new PiiMasker();

    @Test
    void maskPhone() {
        assertEquals("手机 138****1234", m.mask("手机 13812341234"));
    }

    @Test
    void maskIdCard() {
        assertEquals("身份证 110***********1234", m.mask("身份证 110101199001011234"));
    }

    @Test
    void keepPlain() {
        assertEquals("信用分 4", m.mask("信用分 4"));
    }
}
