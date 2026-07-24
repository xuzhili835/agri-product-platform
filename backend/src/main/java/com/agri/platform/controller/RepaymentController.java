package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.RepaymentRejectRequest;
import com.agri.platform.dto.RepaymentSubmitRequest;
import com.agri.platform.entity.Repayment;
import com.agri.platform.service.RepaymentService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 还款计划：查看某笔融资的各期计划；农户提交还款（附凭证），银行确认/驳回。
 */
@RestController
@RequestMapping("/repayment")
public class RepaymentController {

    @Autowired
    private RepaymentService repaymentService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 某笔融资的还款计划（按期数升序，含逾期动态判定） */
    @GetMapping("/finance/{financeId}")
    public Result<List<Repayment>> listByFinance(@PathVariable Integer financeId) {
        return Result.success(repaymentService.listByFinance(financeId));
    }

    /** 农户提交还款：填流水号 + 凭证 → 进入「待确认」，等待银行核验 */
    @PostMapping("/{id}/pay")
    public Result<String> submit(@RequestHeader("Authorization") String token,
                                  @PathVariable Integer id,
                                  @RequestBody(required = false) RepaymentSubmitRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        repaymentService.submit(id, userName, request);
        return Result.success("还款已提交，等待银行确认");
    }

    /** 银行审核 - 还款列表（默认「待确认」队列，status 可选 1已还/3已驳回 查历史） */
    @GetMapping("/bank/list")
    public Result<Page<Repayment>> bankList(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(repaymentService.bankList(status, page, pageSize));
    }

    /** 银行确认还款：置为已还，农户信用分 +1 */
    @PostMapping("/bank/{id}/confirm")
    public Result<String> bankConfirm(@PathVariable Integer id) {
        repaymentService.bankConfirm(id);
        return Result.success("已确认还款");
    }

    /** 银行驳回还款：置为已驳回，通知农户重新还款 */
    @PostMapping("/bank/{id}/reject")
    public Result<String> bankReject(@PathVariable Integer id,
                                      @RequestBody(required = false) RepaymentRejectRequest request) {
        repaymentService.bankReject(id, request);
        return Result.success("已驳回还款");
    }
}
