package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.service.AlipayService;
import com.agri.platform.service.OrderService;
import com.agri.platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付接口
 * - /alipay/pay    下单并生成跳转收银台的页面（需登录）
 * - /alipay/query  主动查询支付结果并落库（需登录）
 * - /alipay/notify 异步回调（无 JWT，已在 WebConfig 放行；本地开发不触发）
 */
@RestController
@RequestMapping("/alipay")
public class PayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发起支付：返回自动提交到支付宝收银台的 HTML 表单（前端注入隐藏容器后自动 submit）。
     */
    @PostMapping("/pay")
    public Result<Map<String, String>> pay(@RequestHeader("Authorization") String token,
                                           @RequestBody Map<String, Integer> body) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Integer purchaseId = body.get("purchaseId");
        if (purchaseId == null) {
            throw new RuntimeException("缺少订单ID");
        }
        String qrCode = alipayService.createQrCode(purchaseId, userName);
        Map<String, String> data = new HashMap<>();
        data.put("qrCode", qrCode);
        return Result.success("下单成功", data);
    }

    /**
     * 主动查询支付结果并落库。复用 getOrderDetail 做订单归属校验。
     */
    @GetMapping("/query")
    public Result<Map<String, Object>> query(@RequestHeader("Authorization") String token,
                                             @RequestParam Integer purchaseId) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        orderService.getOrderDetail(purchaseId, userName); // 校验归属，非本人则抛异常
        return Result.success(alipayService.queryAndMarkPaid(purchaseId));
    }

    /**
     * 支付宝异步回调。返回纯文本 "success"/"fail"（支付宝约定）。
     */
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(@RequestParam Map<String, String> params) {
        return alipayService.handleNotify(params);
    }
}
