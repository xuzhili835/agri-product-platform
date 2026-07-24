package com.agri.platform.service.impl;

import com.agri.platform.config.AlipayProperties;
import com.agri.platform.entity.Payment;
import com.agri.platform.entity.Purchase;
import com.agri.platform.mapper.PaymentMapper;
import com.agri.platform.service.AlipayService;
import com.agri.platform.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentMapper paymentMapper;

    private AlipayClient buildClient() {
        return new DefaultAlipayClient(
                alipayProperties.getGateway(),
                alipayProperties.getAppId(),
                alipayProperties.getAppPrivateKey(),
                alipayProperties.getFormat(),
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
        );
    }

    @Override
    public String createQrCode(Integer purchaseId, String userName) {
        // 校验订单归属：非本人订单会抛异常
        Purchase order = orderService.getOrderDetail(purchaseId, userName);
        Integer status = order.getPurchaseStatus();
        if (status == null || status != 1) {
            throw new RuntimeException("订单当前状态不允许支付");
        }

        BigDecimal totalAmount = order.getTotalPrice() == null
                ? BigDecimal.ZERO
                : order.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
        String amount = totalAmount.toPlainString();

        // 商户订单号：每次发起支付都唯一（purchaseId + 时间戳），避免对同一订单重复发起时
        // 支付宝侧 out_trade_no 冲突（表现为扫码后「订单不存在」）。回调/查询通过 tb_payment 反查 purchaseId。
        String outTradeNo = purchaseId + "T" + System.currentTimeMillis();
        String subject = "农产品订单#" + purchaseId;

        // 先落一条「待支付」流水，作为回调/主动查询时 out_trade_no → purchaseId 的映射。
        Payment pending = new Payment();
        pending.setPurchaseId(purchaseId);
        pending.setOutTradeNo(outTradeNo);
        pending.setTotalAmount(totalAmount);
        pending.setTradeStatus("WAIT_BUYER_PAY");
        pending.setPayTime(LocalDateTime.now());
        pending.setCreateTime(LocalDateTime.now());
        paymentMapper.insert(pending);

        // 当面付扫码支付（precreate）：返回二维码链接 qr_code，前端渲染成二维码让买家用支付宝App扫码付款
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        String bizContent = "{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + amount + "\","
                + "\"subject\":\"" + subject + "\"}";
        request.setBizContent(bizContent);

        try {
            AlipayTradePrecreateResponse response = buildClient().execute(request);
            if (!response.isSuccess()) {
                throw new RuntimeException("生成支付二维码失败：" + response.getSubMsg());
            }
            return response.getQrCode();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("生成支付二维码失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> queryAndMarkPaid(Integer purchaseId) {
        Map<String, Object> result = new HashMap<>();
        result.put("purchaseId", purchaseId);

        try {
            // 用本订单最近一次发起支付的 out_trade_no 去查支付宝（每次发起都唯一）。
            // 老订单若无流水记录，回退为 purchaseId，兼容历史数据。
            String outTradeNo = latestOutTradeNo(purchaseId);
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
            AlipayTradeQueryResponse response = buildClient().execute(request);

            result.put("tradeStatus", response.getTradeStatus());

            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                orderService.markOrderPaid(purchaseId);
                String amtStr = response.getTotalAmount();
                BigDecimal paidAmount = (amtStr != null && !amtStr.isEmpty())
                        ? new BigDecimal(amtStr) : BigDecimal.ZERO;
                savePaymentRecord(outTradeNo, purchaseId, response.getTradeNo(), paidAmount, response.getTradeStatus(), null);
                result.put("paid", true);
            } else {
                result.put("paid", false);
            }
        } catch (Exception e) {
            result.put("paid", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        try {
            boolean signOk = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
            if (!signOk) {
                return "fail";
            }
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            if ("TRADE_SUCCESS".equals(tradeStatus) && outTradeNo != null && !outTradeNo.isEmpty()) {
                // out_trade_no 形如 "purchaseId T timestamp"；优先按流水反查 purchaseId
                Integer purchaseId = resolvePurchaseId(outTradeNo);
                if (purchaseId == null) {
                    return "fail";
                }
                orderService.markOrderPaid(purchaseId);
                String amtStr = params.get("total_amount");
                BigDecimal paidAmount = (amtStr != null && !amtStr.isEmpty())
                        ? new BigDecimal(amtStr) : BigDecimal.ZERO;
                savePaymentRecord(outTradeNo, purchaseId, params.get("trade_no"), paidAmount, tradeStatus, params.get("gmt_payment"));
            }
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }

    /** 取本订单最近一次发起支付使用的 out_trade_no（paymentId 最大的一条），无则回退 purchaseId。 */
    private String latestOutTradeNo(Integer purchaseId) {
        Payment latest = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPurchaseId, purchaseId)
                        .orderByDesc(Payment::getPaymentId)
                        .last("LIMIT 1"));
        return latest != null && latest.getOutTradeNo() != null ? latest.getOutTradeNo() : String.valueOf(purchaseId);
    }

    /** 由 out_trade_no 反查 purchaseId：先查 tb_payment，再按 "purchaseId T timestamp" 前缀解析，最后回退整数解析。 */
    private Integer resolvePurchaseId(String outTradeNo) {
        Payment p = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOutTradeNo, outTradeNo).last("LIMIT 1"));
        if (p != null && p.getPurchaseId() != null) {
            return p.getPurchaseId();
        }
        int tIdx = outTradeNo.indexOf('T');
        String head = tIdx > 0 ? outTradeNo.substring(0, tIdx) : outTradeNo;
        try {
            return Integer.valueOf(head);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 幂等记录支付流水：按 out_trade_no（每次发起支付唯一）查重，存在则更新，不存在则插入。
     * 主动查询走 gmtPayment=null（用当前时间）；异步回调带支付宝返回的 gmt_payment。
     */
    private void savePaymentRecord(String outTradeNo, Integer purchaseId, String alipayTradeNo,
                                   BigDecimal totalAmount, String tradeStatus, String gmtPayment) {
        Payment existing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOutTradeNo, outTradeNo).last("LIMIT 1"));
        LocalDateTime payTime = parsePayTime(gmtPayment);
        if (existing == null) {
            Payment payment = new Payment();
            payment.setPurchaseId(purchaseId);
            payment.setOutTradeNo(outTradeNo);
            payment.setAlipayTradeNo(alipayTradeNo);
            payment.setTotalAmount(totalAmount);
            payment.setTradeStatus(tradeStatus);
            payment.setPayTime(payTime);
            payment.setCreateTime(LocalDateTime.now());
            paymentMapper.insert(payment);
        } else {
            existing.setAlipayTradeNo(alipayTradeNo);
            existing.setTotalAmount(totalAmount);
            existing.setTradeStatus(tradeStatus);
            existing.setPayTime(payTime);
            paymentMapper.updateById(existing);
        }
    }

    /** 解析支付宝回调里的 gmt_payment（形如 2024-01-01 12:00:00，可能带毫秒/时区后缀），失败回退当前时间。 */
    private LocalDateTime parsePayTime(String gmtPayment) {
        if (gmtPayment == null || gmtPayment.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(gmtPayment.substring(0, 19),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
