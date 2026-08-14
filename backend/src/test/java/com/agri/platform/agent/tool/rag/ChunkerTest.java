package com.agri.platform.agent.tool.rag;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChunkerTest {
    private final Chunker c = new Chunker();
    @Test void shortTextOneChunk() { assertEquals(1, c.chunk("短文本").size()); }
    @Test void longTextMultiple() {
        String t = "x".repeat(700);
        List<String> cs = c.chunk(t);
        assertTrue(cs.size() >= 3);
        assertTrue(cs.get(0).length() <= 300);
    }
    @Test void empty() { assertTrue(c.chunk("").isEmpty()); }
}
