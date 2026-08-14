package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_knowledge_chunk")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer knowledgeId;
    private Integer chunkIndex;
    private String content;
    private byte[] embedding;       // LONGBLOB
    private String roleScope;
    private String model;
    private LocalDateTime createTime;
}
