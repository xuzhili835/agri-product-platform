package com.agri.platform.agent.tool;

import com.agri.platform.agent.dto.ToolSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    static Tool fake(String name, String role) {
        return new Tool() {
            public String name() { return name; }
            public ToolSpec spec() { return ToolSpec.builder().name(name).description("").parameters(Map.of()).build(); }
            public String role() { return role; }
            public boolean isWrite() { return false; }
            public String previewOrExecute(ToolContext c, Map<String,Object> a) { return ""; }
            public String execute(ToolContext c, Map<String,Object> a) { return ""; }
        };
    }

    @Test
    void filtersByRole() {
        ToolRegistry reg = new ToolRegistry(List.of(fake("a", "farmer"), fake("b", "buyer"), fake("c", "common")));
        assertEquals(2, reg.specsForRole("farmer").size());     // a + common
        assertEquals(2, reg.specsForRole("buyer").size());      // b + common
        assertEquals("a", reg.get("a").name());
    }
}
