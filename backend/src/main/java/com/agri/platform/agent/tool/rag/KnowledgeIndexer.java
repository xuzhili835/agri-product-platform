package com.agri.platform.agent.tool.rag;

import com.agri.platform.agent.provider.EmbeddingProvider;
import com.agri.platform.agent.provider.SiliconFlowEmbeddingProvider;
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

import java.util.List;

/** 启动时把知识切块载入内存向量库:tb_knowledge_chunk 有数据则直接载入;为空则对 tb_knowledge 切块+embed 灌库后再载入。 */
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

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        try {
            List<KnowledgeChunk> chunks = chunkMapper.selectList(null);
            if (chunks == null || chunks.isEmpty()) {
                bootstrap();
                chunks = chunkMapper.selectList(null);
            }
            int n = 0;
            for (KnowledgeChunk c : chunks) {
                if (c.getEmbedding() == null) continue;
                vectorStore.add(c.getContent(), SiliconFlowEmbeddingProvider.toFloats(c.getEmbedding()), c.getRoleScope());
                n++;
            }
            log.info("[agent知识库] 已载入 {} 个块", n);
        } catch (Exception e) {
            log.warn("[agent知识库] 载入/索引失败,跳过(不阻断启动): {}", e.getMessage());
        }
    }

    /** 把所有已发布(status=1)的 tb_knowledge 切块、embed、写入 tb_knowledge_chunk。幂等:先按 knowledge_id 清旧块。 */
    private void bootstrap() {
        List<Knowledge> all = knowledgeMapper.selectList(
                new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getStatus, 1));
        if (all == null || all.isEmpty()) { log.warn("[agent知识库] 无已发布文章,跳过 bootstrap"); return; }
        for (Knowledge k : all) {
            chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>().eq(KnowledgeChunk::getKnowledgeId, k.getKnowledgeId()));
            List<String> pieces = chunker.chunk(k.getContent());
            List<float[]> vecs = embeddingProvider.embed(pieces);
            for (int i = 0; i < pieces.size(); i++) {
                KnowledgeChunk c = new KnowledgeChunk();
                c.setKnowledgeId(k.getKnowledgeId());
                c.setChunkIndex(i);
                c.setContent(pieces.get(i));
                c.setEmbedding(SiliconFlowEmbeddingProvider.toBytes(vecs.get(i)));
                c.setRoleScope("common");
                c.setModel("bge-m3");
                chunkMapper.insert(c);
            }
        }
        log.info("[agent知识库] bootstrap 完成");
    }
}
