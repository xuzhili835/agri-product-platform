package com.agri.platform.agent.tool.rag;

import com.agri.platform.agent.provider.EmbeddingProvider;
import com.agri.platform.agent.provider.SiliconFlowEmbeddingProvider;
import com.agri.platform.config.SiliconFlowProperties;
import com.agri.platform.entity.Knowledge;
import com.agri.platform.entity.KnowledgeChunk;
import com.agri.platform.mapper.KnowledgeChunkMapper;
import com.agri.platform.mapper.KnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时做<strong>增量对账</strong>后把知识切块载入内存向量库:
 * <ol>
 *   <li>已发布文章无 chunk → 切块+embed+入库(补新文章);</li>
 *   <li>文章 updateTime 晚于其最新 chunk 的 createTime → 重建该文章的块(内容已更新);</li>
 *   <li>chunk 指向的文章已删除/取消发布 → 清掉孤儿块(防旧内容继续被检索)。</li>
 * </ol>
 * <p>此前逻辑是"chunk 表非空就整体跳过 bootstrap"——只有 chunk 表完全为空的首次启动才会索引,
 * 之后新发布的知识永远进不了 RAG(重启也没用),除非手动清表。单篇文章 embed 失败跳过该篇不阻断其余。</p>
 */
@Slf4j
@Component
@Order(20)   // 晚于 DatabaseMigrationRunner(默认 Order)
@RequiredArgsConstructor
public class KnowledgeIndexer implements ApplicationRunner {

    private final KnowledgeChunkMapper chunkMapper;        // extends BaseMapper<KnowledgeChunk>
    private final KnowledgeMapper knowledgeMapper;         // 既有
    private final EmbeddingProvider embeddingProvider;
    private final Chunker chunker;
    private final InMemoryVectorStore vectorStore;
    private final SiliconFlowProperties props;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        try {
            reconcile();
            loadAll();
        } catch (Exception e) {
            log.warn("[agent知识库] 载入/索引失败,跳过(不阻断启动): {}", e.getMessage());
        }
    }

    /** 增量对账:补新、重建已更新、清理已失效。 */
    private void reconcile() {
        List<Knowledge> published = knowledgeMapper.selectList(
                new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getStatus, 1));
        Map<Integer, LocalDateTime> chunkLatest = new HashMap<>();
        for (KnowledgeChunk c : chunkMapper.selectList(null)) {
            chunkLatest.merge(c.getKnowledgeId(), c.getCreateTime() == null ? LocalDateTime.MIN : c.getCreateTime(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }

        // 1) 清孤儿块:文章已删/下架
        Map<Integer, Knowledge> publishedById = new HashMap<>();
        for (Knowledge k : published) publishedById.put(k.getKnowledgeId(), k);
        for (Integer kid : chunkLatest.keySet()) {
            if (!publishedById.containsKey(kid)) {
                chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>().eq(KnowledgeChunk::getKnowledgeId, kid));
                log.info("[agent知识库] 文章#{} 已删除/下架,清理其切块", kid);
            }
        }

        // 2) 补新 + 重建内容有更新的
        int indexed = 0, rebuilt = 0, failed = 0;
        for (Knowledge k : published) {
            LocalDateTime latest = chunkLatest.get(k.getKnowledgeId());
            boolean needBuild = latest == null
                    || (k.getUpdateTime() != null && k.getUpdateTime().isAfter(latest));
            if (!needBuild) continue;
            try {
                if (latest != null) {
                    chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                            .eq(KnowledgeChunk::getKnowledgeId, k.getKnowledgeId()));
                    rebuilt++;
                } else {
                    indexed++;
                }
                embedAndStore(k);
            } catch (Exception e) {
                failed++;
                log.warn("[agent知识库] 文章#{} 索引失败,跳过该篇: {}", k.getKnowledgeId(), e.getMessage());
            }
        }
        if (indexed > 0 || rebuilt > 0 || failed > 0) {
            log.info("[agent知识库] 对账完成: 新索引 {} 篇, 重建 {} 篇, 失败 {} 篇", indexed, rebuilt, failed);
        }
    }

    /** 把一篇文章切块、embed、写入 tb_knowledge_chunk。 */
    private void embedAndStore(Knowledge k) {
        List<String> pieces = chunker.chunk(k.getContent());
        if (pieces.isEmpty()) return;
        List<float[]> vecs = embeddingProvider.embed(pieces);
        for (int i = 0; i < pieces.size(); i++) {
            KnowledgeChunk c = new KnowledgeChunk();
            c.setKnowledgeId(k.getKnowledgeId());
            c.setChunkIndex(i);
            c.setContent(pieces.get(i));
            c.setEmbedding(SiliconFlowEmbeddingProvider.toBytes(vecs.get(i)));
            c.setRoleScope("common");
            c.setModel(props.getEmbedModel());
            chunkMapper.insert(c);
        }
    }

    /** 全量载入内存向量库。 */
    private void loadAll() {
        vectorStore.clear();
        List<KnowledgeChunk> chunks = chunkMapper.selectList(null);
        int n = 0;
        for (KnowledgeChunk c : chunks) {
            if (c.getEmbedding() == null) continue;
            vectorStore.add(c.getContent(), SiliconFlowEmbeddingProvider.toFloats(c.getEmbedding()), c.getRoleScope());
            n++;
        }
        log.info("[agent知识库] 已载入 {} 个块", n);
    }
}
