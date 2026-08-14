package com.agri.platform.agent.provider;

import java.util.List;

public interface EmbeddingProvider {
    /** 批量向量化,返回每段文本对应的 float[](bge-m3 为 1024 维)。 */
    List<float[]> embed(List<String> texts);
}
