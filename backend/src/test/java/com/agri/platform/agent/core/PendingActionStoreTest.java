package com.agri.platform.agent.core;

import com.agri.platform.config.SiliconFlowProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingActionStoreTest {
    private final PendingActionStore store = new PendingActionStore(new SiliconFlowProperties());

    @Test
    void putAndGet() {
        PendingActionStore.Pending p = store.put("s1", null, null, java.util.Map.of("x", 1), "draft");
        assertNotNull(p.getId());
        assertSame(p, store.get(p.getId()));
        store.remove(p.getId());
        assertNull(store.get(p.getId()));
    }

    @Test
    void removeBySessionDropsOnlyThatSession() {
        PendingActionStore.Pending a = store.put("s1", null, null, java.util.Map.of(), "d1");
        store.put("s1", null, null, java.util.Map.of(), "d2");
        PendingActionStore.Pending other = store.put("s2", null, null, java.util.Map.of(), "d3");
        store.removeBySession("s1");
        assertNull(store.get(a.getId()), "同 session 的 pending 应全部被清");
        assertNotNull(store.get(other.getId()), "其他 session 的 pending 不受影响");
    }
}
