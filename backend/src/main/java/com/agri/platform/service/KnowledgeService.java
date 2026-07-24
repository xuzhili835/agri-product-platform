package com.agri.platform.service;

import com.agri.platform.dto.KnowledgeRequest;
import com.agri.platform.entity.Discuss;
import com.agri.platform.entity.Knowledge;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface KnowledgeService {
    void publishKnowledge(String userName, KnowledgeRequest request);
    Page<Knowledge> getKnowledgeList(int page, int pageSize);
    Page<Knowledge> getKnowledgeList(int page, int pageSize, Integer status, String ownName);
    Knowledge getKnowledgeById(Integer knowledgeId);
    void updateKnowledge(Integer knowledgeId, String userName, KnowledgeRequest request);
    void deleteKnowledge(Integer knowledgeId, String userName);
    void addDiscuss(Integer knowledgeId, String userName, String content);
    List<Discuss> getDiscussList(Integer knowledgeId);
    void deleteDiscuss(Integer discussId, String userName);
}
