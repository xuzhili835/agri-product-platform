package com.agri.platform.service;

import com.agri.platform.dto.ReserveRequest;
import com.agri.platform.entity.Reserve;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ReserveService {
    void makeReservation(String userName, ReserveRequest request);
    Page<Reserve> getReserveList(String userName, int page, int pageSize);
    Page<Reserve> getReserveListByExpert(String expertName, int page, int pageSize);
    void cancelReserve(Integer reserveId, String userName);
    void confirmReserve(Integer reserveId, String expertName, Integer status);
    void confirmReserve(Integer reserveId, String expertName, Integer status, String answer);

    /**
     * 清理超期未处理的预约：期望时间(preferredTime)已过当前日期、且仍为「待处理」(status=0)的，
     * 置为「已过期」(status=3)并通知预约人。读取预约列表时惰性触发，无需定时任务。
     */
    void sweepOverdue();
}
