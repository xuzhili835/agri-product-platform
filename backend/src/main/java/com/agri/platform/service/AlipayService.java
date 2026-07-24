package com.agri.platform.service;

import java.util.Map;

/**
 * 支付宝支付服务
 */
public interface AlipayService {

    /**
     * 生成支付宝扫码支付二维码链接（当面付 alipay.trade.precreate）。
     * 返回二维码内容 qr_code（前端渲染成二维码图片，买家用支付宝App扫码付款）。
     * 内部会校验订单归属与状态（仅待付款订单可发起）。
     */
    String createQrCode(Integer purchaseId, String userName);

    /**
     * 主动查询某订单的支付结果；若已支付则把订单标记为已付款（待发货）。
     */
    Map<String, Object> queryAndMarkPaid(Integer purchaseId);

    /**
     * 处理支付宝异步回调通知：验签成功且交易为 TRADE_SUCCESS 时标记订单已付款。
     * 返回 "success" / "fail"（支付宝约定）。
     */
    String handleNotify(Map<String, String> params);
}
