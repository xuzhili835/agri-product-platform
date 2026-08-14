package com.agri.platform.agent.tool.rag;

import com.agri.platform.agent.provider.EmbeddingProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryVectorStoreTest {

    EmbeddingProvider fake = texts -> texts.stream().map(t -> new float[]{t.length(), 1f}).toList();
    InMemoryVectorStore store = new InMemoryVectorStore(fake);

    @Test void searchFiltersByRoleAndRanks() {
        store.add("农技A", new float[]{4f, 1f}, "farmer");
        store.add("买家B", new float[]{4f, 1f}, "buyer");
        store.add("公共C", new float[]{5f, 1f}, "common");
        List<String> r = store.search("abcd", "farmer", 3);  // query 长度4 -> vec(4,1)
        assertTrue(r.contains("农技A"));
        assertTrue(r.contains("公共C"));
        assertFalse(r.contains("买家B"));   // farmer 看不到 buyer scope
    }

    @Test void emptyStore() { assertTrue(store.search("x", "farmer", 3).isEmpty()); }
}
