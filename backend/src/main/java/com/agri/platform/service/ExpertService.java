package com.agri.platform.service;

import com.agri.platform.dto.ExpertRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.Reserve;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ExpertService {
    void updateExpert(String userName, ExpertRequest request);
    Expert getExpertByUserName(String userName);
    Page<Reserve> getReserveList(String expertName, int page, int pageSize, Integer status);
    void confirmReserve(Integer reserveId, String expertName, Integer status);
    void confirmReserve(Integer reserveId, String expertName, Integer status, String answer);
    List<Expert> getAllExperts();
}
