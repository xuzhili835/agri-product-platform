package com.agri.platform.dto;

import lombok.Data;

@Data
public class KnowledgeRequest {
    private Integer knowledgeId;
    private String title;
    private String content;
    private String picPath;
    private Integer status; // 1发布 0草稿
}
