package com.agri.platform.service;

import com.agri.platform.dto.RepaymentRejectRequest;
import com.agri.platform.dto.RepaymentSubmitRequest;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.Repayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 还款计划服务：融资通过时生成等额本息计划，农户逐期提交还款（附凭证），银行确认后计入信用。
 */
public interface RepaymentService {

    /** 融资通过时按等额本息生成还款计划（已存在则跳过，幂等） */
    void generatePlan(Finance finance);

    /** 某笔融资的还款计划（按期数升序，动态判定逾期） */
    List<Repayment> listByFinance(Integer financeId);

    /**
     * 农户提交某期还款：填流水号 + 上传凭证 → status 置为「待确认(2)」，等待银行核验。
     * 仅 status∈{0未还, 3已驳回} 可提交。
     */
    void submit(Integer repaymentId, String userName, RepaymentSubmitRequest request);

    /**
     * 银行审核：还款列表（默认「待确认」，可按状态筛选），回填 farmerName，按提交时间倒序。
     */
    Page<Repayment> bankList(Integer status, int page, int pageSize);

    /**
     * 银行确认还款：status 2待确认 → 1已还，并给农户信用分 +1（封顶5）。
     */
    void bankConfirm(Integer repaymentId);

    /**
     * 银行驳回还款：status 2待确认 → 3已驳回，记录驳回原因，通知农户重新还款。
     */
    void bankReject(Integer repaymentId, RepaymentRejectRequest request);
}
