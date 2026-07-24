package com.agri.platform.service.impl;

import com.agri.platform.dto.FinancingIntentionRequest;
import com.agri.platform.entity.FinancingIntention;
import com.agri.platform.mapper.FinancingIntentionMapper;
import com.agri.platform.service.FinancingIntentionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancingIntentionServiceImpl implements FinancingIntentionService {

    @Autowired
    private FinancingIntentionMapper intentionMapper;

    @Override
    public void submitIntention(String userName, FinancingIntentionRequest request) {
        FinancingIntention intention = new FinancingIntention();
        intention.setUserName(userName);
        intention.setRealName(request.getRealName());
        intention.setPhone(request.getPhone());
        intention.setAddress(request.getAddress());
        intention.setAmount(request.getAmount());
        intention.setApplication(request.getApplication());
        intention.setItem(request.getItem());
        intention.setArea(request.getArea());
        intention.setRepaymentPeriod(request.getRepaymentPeriod());
        intentionMapper.insert(intention);
    }

    @Override
    public Page<FinancingIntention> getIntentionList(int page, int pageSize) {
        Page<FinancingIntention> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<FinancingIntention> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FinancingIntention::getCreateTime);
        return intentionMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<FinancingIntention> getMatchedList(int minAmount, int maxAmount, String item, int page, int pageSize) {
        Page<FinancingIntention> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<FinancingIntention> wrapper = new LambdaQueryWrapper<>();

        // 智能匹配：金额范围和农作物类型
        if (minAmount > 0) {
            wrapper.ge(FinancingIntention::getAmount, minAmount);
        }
        if (maxAmount > 0) {
            wrapper.le(FinancingIntention::getAmount, maxAmount);
        }
        if (item != null && !item.isEmpty()) {
            wrapper.like(FinancingIntention::getItem, item);
        }

        wrapper.orderByDesc(FinancingIntention::getCreateTime);
        return intentionMapper.selectPage(pageParam, wrapper);
    }
}