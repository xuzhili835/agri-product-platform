package com.agri.platform.service;

import com.agri.platform.dto.FinancingIntentionRequest;
import com.agri.platform.entity.FinancingIntention;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FinancingIntentionService {
    void submitIntention(String userName, FinancingIntentionRequest request);
    Page<FinancingIntention> getIntentionList(int page, int pageSize);
    Page<FinancingIntention> getMatchedList(int minAmount, int maxAmount, String item, int page, int pageSize);
}