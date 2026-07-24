package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.OrderRequest;
import com.agri.platform.dto.OrderResponse;
import com.agri.platform.dto.OrderPageResponse;
import com.agri.platform.entity.Purchase;
import com.agri.platform.service.OrderService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<Integer> submit(@RequestHeader("Authorization") String token,
                                   @RequestBody OrderRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Integer orderId = orderService.submitOrder(userName, request);
        return Result.success("下单成功", orderId);
    }

    @GetMapping("/list")
    public Result<Page<Purchase>> list(@RequestHeader("Authorization") String token,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(orderService.getOrderList(userName, page, pageSize));
    }

    @GetMapping("/list/details")
    public Result<List<OrderResponse>> listWithDetails(@RequestHeader("Authorization") String token,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(orderService.getOrderListWithDetails(userName, page, pageSize));
    }

    @GetMapping("/list/paged")
    public Result<OrderPageResponse> listWithDetailsPaged(@RequestHeader("Authorization") String token,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize,
                                                          @RequestParam(required = false) Integer status) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(orderService.getOrderListWithDetailsPaged(userName, page, pageSize, status));
    }

    @GetMapping("/received/paged")
    public Result<OrderPageResponse> getReceivedOrders(@RequestHeader("Authorization") String token,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) Integer status) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(orderService.getReceivedOrdersPaged(userName, page, pageSize, status));
    }

    @GetMapping("/{purchaseId}")
    public Result<OrderResponse> detail(@PathVariable Integer purchaseId,
                                     @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(orderService.getOrderDetailWithDetails(purchaseId, userName));
    }

    @PutMapping("/{purchaseId}/status")
    public Result<String> updateStatus(@PathVariable Integer purchaseId,
                                        @RequestHeader("Authorization") String token,
                                        @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        orderService.updateOrderStatus(purchaseId, status, userName);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{purchaseId}")
    public Result<String> cancel(@PathVariable Integer purchaseId,
                                   @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        orderService.cancelOrder(purchaseId, userName);
        return Result.success("订单已取消");
    }
}