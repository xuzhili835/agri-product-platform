package com.agri.platform.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingActionStoreTest {
    private final PendingActionStore store = new PendingActionStore();

    @Test
    void putAndGet() {
        PendingActionStore.Pending p = store.put("s1", null, null, java.util.Map.of("x", 1), "draft");
        assertNotNull(p.getId());
        assertSame(p, store.get(p.getId()));
        store.remove(p.getId());
        assertNull(store.get(p.getId()));
    }
}
