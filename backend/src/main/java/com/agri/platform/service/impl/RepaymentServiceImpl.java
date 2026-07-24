package com.agri.platform.service.impl;

import com.agri.platform.dto.RepaymentRejectRequest;
import com.agri.platform.dto.RepaymentSubmitRequest;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.Repayment;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.FinanceMapper;
import com.agri.platform.mapper.RepaymentMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.RepaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RepaymentServiceImpl implements RepaymentService {

    @Autowired
    private RepaymentMapper repaymentMapper;

    @Autowired
    private FinanceMapper financeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public void generatePlan(Finance finance) {
        if (finance == null || finance.getFinanceId() == null) {
            return;
        }
        // 幂等：已有计划则跳过
        Long exists = repaymentMapper.selectCount(
                new LambdaQueryWrapper<Repayment>().eq(Repayment::getFinanceId, finance.getFinanceId()));
        if (exists != null && exists > 0) {
            return;
        }

        BigDecimal principal = finance.getMoney() == null ? BigDecimal.ZERO : finance.getMoney();
        int periods = finance.getRepayment() == null ? 0 : finance.getRepayment();
        if (periods <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // 月利率 = 年利率% / 12 / 100（rate 字段为百分数，如 5 表示 5%）
        BigDecimal annualRate = finance.getRate() == null ? BigDecimal.ZERO : finance.getRate();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100 * 12), 10, RoundingMode.HALF_UP);

        // 等额本息月供 = P*r*(1+r)^n / ((1+r)^n - 1)；r=0 时为 P/n
        BigDecimal monthlyPayment;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = principal.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal factor = BigDecimal.ONE.add(monthlyRate); // (1+r)^1
            for (int i = 1; i < periods; i++) {
                factor = factor.multiply(BigDecimal.ONE.add(monthlyRate));
            } // factor = (1+r)^n
            monthlyPayment = principal.multiply(monthlyRate).multiply(factor)
                    .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        }

        LocalDate base = LocalDate.now();
        BigDecimal balance = principal;
        for (int i = 1; i <= periods; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart;
            BigDecimal total;
            if (i == periods) {
                // 末期本金取剩余余额，消除累计舍入误差
                principalPart = balance;
                total = principalPart.add(interest);
            } else {
                principalPart = monthlyPayment.subtract(interest);
                total = monthlyPayment;
            }
            balance = balance.subtract(principalPart);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }

            Repayment r = new Repayment();
            r.setFinanceId(finance.getFinanceId());
            r.setPeriodIndex(i);
            r.setDueDate(base.plusMonths(i));
            r.setPrincipal(principalPart.setScale(2, RoundingMode.HALF_UP));
            r.setInterest(interest);
            r.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
            r.setPaidAmount(BigDecimal.ZERO);
            r.setStatus(0);
            r.setCreateTime(LocalDateTime.now());
            repaymentMapper.insert(r);
        }
    }

    @Override
    public List<Repayment> listByFinance(Integer financeId) {
        LambdaQueryWrapper<Repayment> w = new LambdaQueryWrapper<>();
        w.eq(Repayment::getFinanceId, financeId).orderByAsc(Repayment::getPeriodIndex);
        List<Repayment> list = repaymentMapper.selectList(w);
        LocalDate today = LocalDate.now();
        for (Repayment r : list) {
            // 逾期：未还(0) 或 已驳回(3) 且到期日早于今天
            Integer st = r.getStatus();
            boolean unpaid = st != null && (st == 0 || st == 3);
            boolean od = unpaid && r.getDueDate() != null && r.getDueDate().isBefore(today);
            r.setOverdue(od);
        }
        return list;
    }

    @Override
    @Transactional
    public void submit(Integer repaymentId, String userName, RepaymentSubmitRequest request) {
        Repayment r = repaymentMapper.selectById(repaymentId);
        if (r == null) {
            throw new RuntimeException("还款记录不存在");
        }
        Finance finance = financeMapper.selectById(r.getFinanceId());
        if (finance == null) {
            throw new RuntimeException("融资申请不存在");
        }
        if (!finance.getOwnName().equals(userName)) {
            throw new RuntimeException("无权操作此还款");
        }
        Integer st = r.getStatus();
        if (st != null && st == 1) {
            throw new RuntimeException("该期已还款");
        }
        if (st != null && st == 2) {
            throw new RuntimeException("该期还款已提交，待银行确认");
        }
        // 仅 0未还 / 3已驳回 可提交

        r.setStatus(2); // 待确认
        r.setPaidAmount(r.getTotalAmount());
        r.setPaidTime(LocalDateTime.now());
        if (request != null) {
            r.setTransactionNo(request.getTransactionNo());
            r.setPayProof(request.getPayProof());
        }
        r.setRejectReason(null); // 清掉历史驳回原因
        repaymentMapper.updateById(r);

        messageService.send(userName, "finance",
                "还款已提交",
                "您的融资申请 #" + finance.getFinanceId() + " 第 " + r.getPeriodIndex()
                        + " 期还款已提交（¥" + r.getTotalAmount() + "），等待银行确认。",
                "/farmer/my-finance");
    }

    @Override
    public Page<Repayment> bankList(Integer status, int page, int pageSize) {
        Page<Repayment> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Repayment> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(Repayment::getStatus, status);
        } else {
            w.eq(Repayment::getStatus, 2); // 默认查看「待确认」队列
        }
        w.orderByDesc(Repayment::getPaidTime);
        Page<Repayment> result = repaymentMapper.selectPage(pageParam, w);
        fillFarmerName(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void bankConfirm(Integer repaymentId) {
        Repayment r = repaymentMapper.selectById(repaymentId);
        if (r == null) {
            throw new RuntimeException("还款记录不存在");
        }
        if (r.getStatus() == null || r.getStatus() != 2) {
            throw new RuntimeException("仅「待确认」的还款可确认");
        }
        r.setStatus(1); // 已还
        repaymentMapper.updateById(r);

        Finance finance = financeMapper.selectById(r.getFinanceId());
        if (finance != null) {
            // 信用闭环：每期还款被银行确认，农户信用分 +1（封顶5）—— 按时还款提升信用
            User farmer = userMapper.selectById(finance.getOwnName());
            if (farmer != null) {
                int c = farmer.getCredit() == null ? 5 : farmer.getCredit();
                farmer.setCredit(Math.min(5, c + 1));
                userMapper.updateById(farmer);
            }
            messageService.send(finance.getOwnName(), "finance",
                    "还款已确认",
                    "您的融资申请 #" + finance.getFinanceId() + " 第 " + r.getPeriodIndex()
                            + " 期还款（¥" + r.getTotalAmount() + "）已被银行确认，信用分 +1。",
                    "/farmer/my-finance");
        }
    }

    @Override
    @Transactional
    public void bankReject(Integer repaymentId, RepaymentRejectRequest request) {
        Repayment r = repaymentMapper.selectById(repaymentId);
        if (r == null) {
            throw new RuntimeException("还款记录不存在");
        }
        if (r.getStatus() == null || r.getStatus() != 2) {
            throw new RuntimeException("仅「待确认」的还款可驳回");
        }
        String reason = request != null && request.getReason() != null ? request.getReason().trim() : "";
        r.setStatus(3); // 已驳回
        r.setRejectReason(reason.isEmpty() ? "未通过银行核验" : reason);
        // 驳回 = 视为未实际还款，清零已还金额与时间（保留流水号/凭证作审计留痕）
        r.setPaidAmount(BigDecimal.ZERO);
        r.setPaidTime(null);
        repaymentMapper.updateById(r);

        Finance finance = financeMapper.selectById(r.getFinanceId());
        if (finance != null) {
            messageService.send(finance.getOwnName(), "finance",
                    "还款被驳回",
                    "您的融资申请 #" + finance.getFinanceId() + " 第 " + r.getPeriodIndex()
                            + " 期还款被银行驳回：" + r.getRejectReason() + "，请核实后重新还款。",
                    "/farmer/my-finance");
        }
    }

    /** 批量回填还款记录对应的农户真名（由 financeId 反查 tb_finance.realName），供银行审核列表展示 */
    private void fillFarmerName(List<Repayment> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Integer> financeIds = list.stream()
                .map(Repayment::getFinanceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (financeIds.isEmpty()) {
            return;
        }
        List<Finance> finances = financeMapper.selectBatchIds(financeIds);
        Map<Integer, String> nameMap = finances.stream()
                .collect(Collectors.toMap(Finance::getFinanceId,
                        f -> f.getRealName() != null ? f.getRealName() : f.getOwnName(),
                        (a, b) -> a));
        for (Repayment r : list) {
            r.setFarmerName(nameMap.get(r.getFinanceId()));
        }
    }
}
