package com.agri.platform.agent.tool.rag;

import com.agri.platform.agent.provider.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** 内存向量库:存(文本,向量,roleScope)。检索前按角色过滤,再余弦相似 top-k。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryVectorStore {

    private static class Doc { String text; float[] vec; String role; }
    private final List<Doc> docs = new ArrayList<>();
    private final EmbeddingProvider embeddingProvider;

    public synchronized void add(String text, float[] vec, String role) {
        Doc d = new Doc();
        d.text = text; d.vec = vec; d.role = role == null ? "common" : role;
        docs.add(d);
    }

    public synchronized int size() { return docs.size(); }
    public synchronized void clear() { docs.clear(); }

    public List<String> search(String query, String role, int k) {
        if (docs.isEmpty()) return Collections.emptyList();
        List<float[]> q = embeddingProvider.embed(List.of(query));
        if (q.isEmpty()) return Collections.emptyList();
        float[] qv = q.get(0);
        return docs.stream()
                .filter(d -> "common".equals(d.role) || d.role.equals(role))
                .map(d -> Map.entry(d, cosine(qv, d.vec)))
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .limit(k)
                .map(e -> e.getKey().text)
                .collect(Collectors.toList());
    }

    static float cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na == 0 || nb == 0) return 0f;
        return (float)(dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
