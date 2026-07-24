package com.agri.platform.service;

import com.agri.platform.dto.OrderRequest;
import com.agri.platform.dto.OrderResponse;
import com.agri.platform.dto.OrderPageResponse;
import com.agri.platform.entity.Purchase;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface OrderService {
    Integer submitOrder(String userName, OrderRequest request);
    Page<Purchase> getOrderList(String userName, int page, int pageSize);
    List<OrderResponse> getOrderListWithDetails(String userName, int page, int pageSize);
    OrderPageResponse getOrderListWithDetailsPaged(String userName, int page, int pageSize, Integer status);
    Purchase getOrderDetail(Integer purchaseId, String userName);
    OrderResponse getOrderDetailWithDetails(Integer purchaseId, String userName);
    /** 推进订单状态：status=3 发货（仅卖家）、status=4 确认收货（仅买家）；含所有权与状态机校验。 */
    void updateOrderStatus(Integer purchaseId, Integer status, String userName);
    void cancelOrder(Integer purchaseId, String userName);
    /** 支付成功后把订单从待付款(1)推进到待发货(2)；幂等，非待付款状态不处理。 */
    void markOrderPaid(Integer purchaseId);
    /** 获取农户作为卖家收到的订单（别人购买农户商品的订单） */
    OrderPageResponse getReceivedOrdersPaged(String userName, int page, int pageSize, Integer status);
}