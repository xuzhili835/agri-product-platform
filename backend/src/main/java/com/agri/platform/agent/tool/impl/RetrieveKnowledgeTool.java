package com.agri.platform.agent.tool.impl;

import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.tool.rag.InMemoryVectorStore;
import com.agri.platform.agent.util.Args;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RetrieveKnowledgeTool implements Tool {
    private final InMemoryVectorStore vectorStore;
    public String name() { return "retrieve_knowledge"; }
    public String role() { return "common"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("检索平台知识库(融资政策、操作指南、农技)。参数:query(问题)。")
                .parameters(Map.of("query", "string")).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        String query = Args.str(args.get("query"));
        List<String> passages = vectorStore.search(query == null ? "" : query, ctx.getRole(), 3);
        if (passages.isEmpty()) return "暂无相关知识";
        return "相关知识:\n" + String.join("\n---\n", passages);
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
