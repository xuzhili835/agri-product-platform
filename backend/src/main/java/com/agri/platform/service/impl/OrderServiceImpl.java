package com.agri.platform.service.impl;

import com.agri.platform.dto.OrderRequest;
import com.agri.platform.dto.OrderResponse;
import com.agri.platform.dto.OrderPageResponse;
import com.agri.platform.entity.*;
import com.agri.platform.mapper.*;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private PurchaseMapper purchaseMapper;

    @Autowired
    private PurchaseDetailMapper detailMapper;

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    // 订单状态文本映射
    private static final Map<Integer, String> STATUS_TEXT_MAP = Map.of(
        1, "待付款",
        2, "待发货",
        3, "待收货",
        4, "已完成",
        5, "已取消"
    );

    @Override
    @Transactional
    public Integer submitOrder(String userName, OrderRequest request) {
        // 仅买家可下单购买（农户/专家/银行/管理员不可）
        User buyer = userMapper.selectById(userName);
        if (buyer == null || !"buyer".equals(buyer.getRole())) {
            throw new RuntimeException("仅买家账号可下单购买");
        }

        // 创建订单主记录
        Purchase purchase = new Purchase();
        purchase.setOwnName(userName);
        purchase.setAddress(request.getAddress());
        purchase.setPurchaseType(request.getPurchaseType());
        purchase.setPurchaseStatus(1); // 待付款
        purchase.setTotalPrice(BigDecimal.ZERO); // 占位：total_price 在 DB 里非空，先给 0，下面算完总价会 updateById 覆盖
        purchaseMapper.insert(purchase);

        BigDecimal totalPrice = BigDecimal.ZERO;

        if (request.getPurchaseType() == 1) {
            // 从购物车获取
            List<ShoppingCart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<ShoppingCart>()
                    .eq(ShoppingCart::getOwnName, userName)
            );

            for (ShoppingCart cart : cartItems) {
                Product product = productMapper.selectById(cart.getOrderId());
                if (product == null) continue;

                BigDecimal lineTotal = product.getPrice().multiply(new BigDecimal(cart.getCount()));
                totalPrice = totalPrice.add(lineTotal);

                // 创建订单详情
                PurchaseDetail detail = new PurchaseDetail();
                detail.setPurchaseId(purchase.getPurchaseId());
                detail.setOrderId(cart.getOrderId());
                detail.setUninPrice(product.getPrice());
                detail.setCount(cart.getCount());
                detail.setSumPrice(lineTotal);
                detailMapper.insert(detail);

                // 清空购物车
                cartMapper.deleteById(cart.getShoppingId());
            }
        } else {
            // 直接购买
            for (OrderRequest.OrderItem item : request.getItems()) {
                Product product = productMapper.selectById(item.getOrderId());
                if (product == null) continue;

                BigDecimal lineTotal = product.getPrice().multiply(new BigDecimal(item.getCount()));
                totalPrice = totalPrice.add(lineTotal);

                // 创建订单详情
                PurchaseDetail detail = new PurchaseDetail();
                detail.setPurchaseId(purchase.getPurchaseId());
                detail.setOrderId(item.getOrderId());
                detail.setUninPrice(product.getPrice());
                detail.setCount(item.getCount());
                detail.setSumPrice(lineTotal);
                detailMapper.insert(detail);
            }
        }

        // 更新订单总价
        purchase.setTotalPrice(totalPrice);
        purchaseMapper.updateById(purchase);

        return purchase.getPurchaseId();
    }

    @Override
    public Page<Purchase> getOrderList(String userName, int page, int pageSize) {
        Page<Purchase> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getOwnName, userName);
        wrapper.orderByDesc(Purchase::getCreateTime);
        return purchaseMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Purchase getOrderDetail(Integer purchaseId, String userName) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!purchase.getOwnName().equals(userName)) {
            throw new RuntimeException("无权限查看此订单");
        }
        return purchase;
    }

    @Override
    public OrderResponse getOrderDetailWithDetails(Integer purchaseId, String userName) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!purchase.getOwnName().equals(userName)) {
            throw new RuntimeException("无权限查看此订单");
        }

        // 获取订单详情
        LambdaQueryWrapper<PurchaseDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(PurchaseDetail::getPurchaseId, purchaseId);
        List<PurchaseDetail> details = detailMapper.selectList(detailWrapper);

        // 获取商品信息
        List<Integer> productIds = details.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
            productWrapper.in(Product::getOrderId, productIds);
            List<Product> products = productMapper.selectList(productWrapper);
            productMap = products.stream()
                    .collect(Collectors.toMap(Product::getOrderId, p -> p));
        }

        // 构建响应
        OrderResponse response = new OrderResponse();
        response.setPurchaseId(purchase.getPurchaseId());
        response.setOwnName(purchase.getOwnName());
        response.setTotalPrice(purchase.getTotalPrice());
        response.setAddress(purchase.getAddress());
        response.setPurchaseStatus(purchase.getPurchaseStatus());
        response.setStatusText(STATUS_TEXT_MAP.getOrDefault(purchase.getPurchaseStatus(), "未知"));
        response.setPurchaseType(purchase.getPurchaseType());
        response.setCreateTime(purchase.getCreateTime());
        response.setUpdateTime(purchase.getUpdateTime());

        // 添加商品详情
        List<OrderResponse.OrderItemResponse> items = new ArrayList<>();
        for (PurchaseDetail detail : details) {
            OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
            Product product = productMap.get(detail.getOrderId());
            if (product != null) {
                item.setProductId(product.getOrderId());
                item.setProductName(product.getTitle());
                item.setProductPic(product.getPicPath());
                item.setPrice(detail.getUninPrice());
                item.setCount(detail.getCount());
                item.setTotalPrice(detail.getSumPrice());
            } else {
                // 即使商品不存在，也添加基本详情
                item.setPrice(detail.getUninPrice());
                item.setCount(detail.getCount());
                item.setTotalPrice(detail.getSumPrice());
                item.setProductName("商品已下架");
            }
            items.add(item);
        }
        response.setSellerName(resolveSellerNames(details, productMap));
        response.setItems(items);

        return response;
    }

    @Override
    public void markOrderPaid(Integer purchaseId) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            return; // 订单不存在，忽略（幂等）
        }
        // 仅待付款订单推进，避免重复回调把已发货/已完成订单改回去
        if (purchase.getPurchaseStatus() != null && purchase.getPurchaseStatus() == 1) {
            purchase.setPurchaseStatus(2); // 待发货
            purchaseMapper.updateById(purchase);
            // 通知买家：支付成功，等待发货
            messageService.send(purchase.getOwnName(), "order",
                    "订单支付成功",
                    "您的订单 #" + purchaseId + " 已支付成功，等待商家发货。",
                    "/order/" + purchaseId);
            // 通知卖家（农户）：有新订单待发货
            for (String seller : getSellerUserNames(purchaseId)) {
                messageService.send(seller, "order",
                        "您有新订单待发货",
                        "订单 #" + purchaseId + " 已付款，请尽快发货。",
                        "/farmer/orders");
            }
        }
    }

    @Override
    public void updateOrderStatus(Integer purchaseId, Integer status, String userName) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new RuntimeException("订单不存在");
        }
        Integer cur = purchase.getPurchaseStatus();
        if (status != null && status == 3) {
            // 发货：仅该订单的卖家（订单中某商品的发布者）可发货，且当前须为待发货(2)
            if (cur == null || cur != 2) {
                throw new RuntimeException("当前订单状态不可发货");
            }
            if (!isSellerOfOrder(purchaseId, userName)) {
                throw new RuntimeException("无权操作该订单");
            }
        } else if (status != null && status == 4) {
            // 确认收货：仅买家(purchase.ownName)可确认，且当前须为待收货(3)
            if (cur == null || cur != 3) {
                throw new RuntimeException("当前订单状态不可确认收货");
            }
            if (!purchase.getOwnName().equals(userName)) {
                throw new RuntimeException("无权确认收货");
            }
        } else {
            // 其他状态变更（取消走 DELETE 端点）一律拒绝，避免越权任意改状态
            throw new RuntimeException("不支持的状态变更");
        }
        purchase.setPurchaseStatus(status);
        purchaseMapper.updateById(purchase);
        // 通知买家订单状态变更（待收货/已完成等）
        String statusText = STATUS_TEXT_MAP.getOrDefault(status, "已更新");
        messageService.send(purchase.getOwnName(), "order",
                "订单状态更新：" + statusText,
                "您的订单 #" + purchaseId + " 状态已更新为「" + statusText + "」。",
                "/order/" + purchaseId);
        // 买家确认收货(4)时，额外通知卖家（农户）：买家已收货，交易完成
        if (status != null && status == 4) {
            for (String seller : getSellerUserNames(purchaseId)) {
                messageService.send(seller, "order",
                        "买家已确认收货",
                        "买家已确认收到订单 #" + purchaseId + " 的商品，交易完成。",
                        "/farmer/orders");
            }
        }
    }

    /**
     * 判断 userName 是否为该订单中某商品的发布方（卖家）。
     * 商品 ownName 存的是真实姓名，因此同时匹配 userName 与 realName。
     */
    private boolean isSellerOfOrder(Integer purchaseId, String userName) {
        User user = userMapper.selectById(userName);
        String realName = user != null ? user.getRealName() : null;
        List<PurchaseDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<PurchaseDetail>().eq(PurchaseDetail::getPurchaseId, purchaseId));
        List<Integer> productIds = details.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return false;
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        for (Product p : products) {
            if (userName.equals(p.getOwnName())) return true;
            if (realName != null && realName.equals(p.getOwnName())) return true;
        }
        return false;
    }

    /**
     * 获取订单中所有商品发布方（卖家/农户）的登录账号列表，用于站内通知。
     * 商品 ownName 存的是真实姓名，因此同时按 userName 与 realName 反查用户。
     */
    private List<String> getSellerUserNames(Integer purchaseId) {
        List<PurchaseDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<PurchaseDetail>().eq(PurchaseDetail::getPurchaseId, purchaseId));
        List<Integer> productIds = details.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        List<String> ownNames = products.stream()
                .map(Product::getOwnName)
                .filter(n -> n != null && !n.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (ownNames.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .and(w -> w.in(User::getUserName, ownNames).or().in(User::getRealName, ownNames)));
        return users.stream()
                .map(User::getUserName)
                .filter(n -> n != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 拼接订单的发货人展示名（买家订单列表用）。商品 ownName 即真实姓名；
     * 一单含多卖家时以「、」连接。
     */
    private String resolveSellerNames(List<PurchaseDetail> orderDetails, Map<Integer, Product> productMap) {
        return orderDetails.stream()
                .map(d -> productMap.get(d.getOrderId()))
                .filter(p -> p != null && p.getOwnName() != null && !p.getOwnName().isEmpty())
                .map(Product::getOwnName)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    @Override
    public void cancelOrder(Integer purchaseId, String userName) {
        Purchase purchase = purchaseMapper.selectById(purchaseId);
        if (purchase == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!purchase.getOwnName().equals(userName)) {
            throw new RuntimeException("无权限取消此订单");
        }
        if (purchase.getPurchaseStatus() != 1) {
            throw new RuntimeException("只有待付款订单可以取消");
        }
        purchase.setPurchaseStatus(5); // 已取消
        purchaseMapper.updateById(purchase);
    }

    @Override
    public List<OrderResponse> getOrderListWithDetails(String userName, int page, int pageSize) {
        // 获取订单列表
        Page<Purchase> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getOwnName, userName);
        wrapper.orderByDesc(Purchase::getCreateTime);
        Page<Purchase> purchasePage = purchaseMapper.selectPage(pageParam, wrapper);

        // 获取所有订单ID
        List<Integer> purchaseIds = purchasePage.getRecords().stream()
                .map(Purchase::getPurchaseId)
                .collect(Collectors.toList());

        if (purchaseIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取订单详情
        LambdaQueryWrapper<PurchaseDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(PurchaseDetail::getPurchaseId, purchaseIds);
        List<PurchaseDetail> details = detailMapper.selectList(detailWrapper);

        // 获取商品信息
        List<Integer> productIds = details.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.in(Product::getOrderId, productIds);
        List<Product> products = productMapper.selectList(productWrapper);

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getOrderId, p -> p));

        // 按订单ID分组详情
        Map<Integer, List<PurchaseDetail>> detailMap = details.stream()
                .collect(Collectors.groupingBy(PurchaseDetail::getPurchaseId));

        // 构建响应
        List<OrderResponse> responses = new ArrayList<>();
        for (Purchase purchase : purchasePage.getRecords()) {
            OrderResponse response = new OrderResponse();
            response.setPurchaseId(purchase.getPurchaseId());
            response.setOwnName(purchase.getOwnName());
            response.setTotalPrice(purchase.getTotalPrice());
            response.setAddress(purchase.getAddress());
            response.setPurchaseStatus(purchase.getPurchaseStatus());
            response.setStatusText(STATUS_TEXT_MAP.getOrDefault(purchase.getPurchaseStatus(), "未知"));
            response.setPurchaseType(purchase.getPurchaseType());
            response.setCreateTime(purchase.getCreateTime());
            response.setUpdateTime(purchase.getUpdateTime());

            // 添加商品详情
            List<PurchaseDetail> orderDetails = detailMap.getOrDefault(purchase.getPurchaseId(), new ArrayList<>());
            List<OrderResponse.OrderItemResponse> items = new ArrayList<>();
            for (PurchaseDetail detail : orderDetails) {
                OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
                Product product = productMap.get(detail.getOrderId());
                if (product != null) {
                    item.setProductId(product.getOrderId());
                    item.setProductName(product.getTitle());
                    item.setProductPic(product.getPicPath());
                    item.setPrice(detail.getUninPrice());
                    item.setCount(detail.getCount());
                    item.setTotalPrice(detail.getSumPrice());
                }
                items.add(item);
            }
            response.setSellerName(resolveSellerNames(orderDetails, productMap));
            response.setItems(items);
            responses.add(response);
        }

        return responses;
    }

    @Override
    public OrderPageResponse getOrderListWithDetailsPaged(String userName, int page, int pageSize, Integer status) {
        // 获取订单列表
        Page<Purchase> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Purchase::getOwnName, userName);
        // 按状态筛选（status 为空时查全部）：1待付款 2待发货 3待收货 4已完成 5已取消
        if (status != null) {
            wrapper.eq(Purchase::getPurchaseStatus, status);
        }

        // 排序：按状态优先级（订单中优先：2待发货、3待收货 > 1待付款 > 4已完成 > 5已取消）
        // 然后按创建时间倒序
        wrapper.orderByAsc(Purchase::getPurchaseStatus)
               .orderByDesc(Purchase::getCreateTime);

        Page<Purchase> purchasePage = purchaseMapper.selectPage(pageParam, wrapper);

        // 获取所有订单ID
        List<Integer> purchaseIds = purchasePage.getRecords().stream()
                .map(Purchase::getPurchaseId)
                .collect(Collectors.toList());

        if (purchaseIds.isEmpty()) {
            return new OrderPageResponse(new ArrayList<>(), 0L, page, pageSize);
        }

        // 获取订单详情
        LambdaQueryWrapper<PurchaseDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(PurchaseDetail::getPurchaseId, purchaseIds);
        List<PurchaseDetail> details = detailMapper.selectList(detailWrapper);

        // 获取商品信息
        List<Integer> productIds = details.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.in(Product::getOrderId, productIds);
        List<Product> products = productMapper.selectList(productWrapper);

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getOrderId, p -> p));

        // 按订单ID分组详情
        Map<Integer, List<PurchaseDetail>> detailMap = details.stream()
                .collect(Collectors.groupingBy(PurchaseDetail::getPurchaseId));

        // 构建响应
        List<OrderResponse> responses = new ArrayList<>();
        for (Purchase purchase : purchasePage.getRecords()) {
            OrderResponse response = new OrderResponse();
            response.setPurchaseId(purchase.getPurchaseId());
            response.setOwnName(purchase.getOwnName());
            response.setTotalPrice(purchase.getTotalPrice());
            response.setAddress(purchase.getAddress());
            response.setPurchaseStatus(purchase.getPurchaseStatus());
            response.setStatusText(STATUS_TEXT_MAP.getOrDefault(purchase.getPurchaseStatus(), "未知"));
            response.setPurchaseType(purchase.getPurchaseType());
            response.setCreateTime(purchase.getCreateTime());
            response.setUpdateTime(purchase.getUpdateTime());

            // 添加商品详情
            List<PurchaseDetail> orderDetails = detailMap.getOrDefault(purchase.getPurchaseId(), new ArrayList<>());
            List<OrderResponse.OrderItemResponse> items = new ArrayList<>();
            for (PurchaseDetail detail : orderDetails) {
                OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
                Product product = productMap.get(detail.getOrderId());
                if (product != null) {
                    item.setProductId(product.getOrderId());
                    item.setProductName(product.getTitle());
                    item.setProductPic(product.getPicPath());
                    item.setPrice(detail.getUninPrice());
                    item.setCount(detail.getCount());
                    item.setTotalPrice(detail.getSumPrice());
                }
                items.add(item);
            }
            response.setSellerName(resolveSellerNames(orderDetails, productMap));
            response.setItems(items);
            responses.add(response);
        }

        return new OrderPageResponse(responses, purchasePage.getTotal(), page, pageSize);
    }

    @Override
    public OrderPageResponse getReceivedOrdersPaged(String userName, int page, int pageSize, Integer status) {
        log.info("获取农户订单列表 - 用户名: {}, 页码: {}, 每页: {}", userName, page, pageSize);

        // 获取用户信息（包括真实姓名）
        User user = userMapper.selectById(userName);
        if (user == null) {
            log.warn("用户 {} 不存在", userName);
            return new OrderPageResponse(new ArrayList<>(), 0L, page, pageSize);
        }

        // 查找该农户发布的所有商品（同时匹配用户名和真实姓名）
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.and(wrapper -> wrapper
                .eq(Product::getOwnName, userName)
                .or()
                .eq(Product::getOwnName, user.getRealName())
        );
        List<Product> farmerProducts = productMapper.selectList(productWrapper);

        log.info("农户 {} (真实姓名: {}) 发布的商品数量: {}", userName, user.getRealName(), farmerProducts.size());

        if (farmerProducts.isEmpty()) {
            log.warn("农户 {} 没有发布任何商品", userName);
            return new OrderPageResponse(new ArrayList<>(), 0L, page, pageSize);
        }

        // 获取商品ID列表
        List<Integer> productIds = farmerProducts.stream()
                .map(Product::getOrderId)
                .collect(Collectors.toList());

        // 找到购买这些商品的订单详情
        LambdaQueryWrapper<PurchaseDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(PurchaseDetail::getOrderId, productIds);
        List<PurchaseDetail> details = detailMapper.selectList(detailWrapper);

        log.info("商品ID列表: {}, 找到的订单详情数量: {}", productIds, details.size());

        if (details.isEmpty()) {
            log.warn("没有找到购买这些商品的订单详情");
            return new OrderPageResponse(new ArrayList<>(), 0L, page, pageSize);
        }

        // 获取唯一的订单ID
        List<Integer> purchaseIds = details.stream()
                .map(PurchaseDetail::getPurchaseId)
                .distinct()
                .collect(Collectors.toList());

        // 分页查询订单
        Page<Purchase> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Purchase> purchaseWrapper = new LambdaQueryWrapper<>();
        purchaseWrapper.in(Purchase::getPurchaseId, purchaseIds);
        // 按状态筛选（status 为空时查全部）：1待付款 2待发货 3待收货 4已完成 5已取消
        if (status != null) {
            purchaseWrapper.eq(Purchase::getPurchaseStatus, status);
        }

        // 排序：按状态优先级（订单中优先：2待发货、3待收货 > 1待付款 > 4已完成 > 5已取消）
        // 然后按创建时间倒序
        purchaseWrapper.orderByAsc(Purchase::getPurchaseStatus)
                      .orderByDesc(Purchase::getCreateTime);

        Page<Purchase> purchasePage = purchaseMapper.selectPage(pageParam, purchaseWrapper);

        if (purchasePage.getRecords().isEmpty()) {
            return new OrderPageResponse(new ArrayList<>(), purchasePage.getTotal(), page, pageSize);
        }

        // 获取当前页订单的详情
        List<Integer> currentPagePurchaseIds = purchasePage.getRecords().stream()
                .map(Purchase::getPurchaseId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<PurchaseDetail> currentPageDetailWrapper = new LambdaQueryWrapper<>();
        currentPageDetailWrapper.in(PurchaseDetail::getPurchaseId, currentPagePurchaseIds);
        List<PurchaseDetail> currentPageDetails = detailMapper.selectList(currentPageDetailWrapper);

        // 获取商品信息
        List<Integer> currentPageProductIds = currentPageDetails.stream()
                .map(PurchaseDetail::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> currentPageProductWrapper = new LambdaQueryWrapper<>();
        currentPageProductWrapper.in(Product::getOrderId, currentPageProductIds);
        List<Product> currentPageProducts = productMapper.selectList(currentPageProductWrapper);

        Map<Integer, Product> productMap = currentPageProducts.stream()
                .collect(Collectors.toMap(Product::getOrderId, p -> p));

        // 按订单ID分组详情
        Map<Integer, List<PurchaseDetail>> detailMap = currentPageDetails.stream()
                .collect(Collectors.groupingBy(PurchaseDetail::getPurchaseId));

        // 批量回填买家真实姓名（订单 ownName 为买家登录账号），便于农户看到买家真名
        List<String> buyerNames = purchasePage.getRecords().stream()
                .map(Purchase::getOwnName)
                .filter(n -> n != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> buyerRealNameMap = new HashMap<>();
        if (!buyerNames.isEmpty()) {
            List<User> buyers = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getUserName, buyerNames)
            );
            buyerRealNameMap = buyers.stream()
                    .filter(u -> u.getRealName() != null)
                    .collect(Collectors.toMap(User::getUserName, User::getRealName, (a, b) -> a));
        }

        // 构建响应 - 只包含该农户的商品
        List<OrderResponse> responses = new ArrayList<>();
        for (Purchase purchase : purchasePage.getRecords()) {
            OrderResponse response = new OrderResponse();
            response.setPurchaseId(purchase.getPurchaseId());
            response.setOwnName(purchase.getOwnName());
            response.setBuyerRealName(buyerRealNameMap.get(purchase.getOwnName()));
            response.setTotalPrice(purchase.getTotalPrice());
            response.setAddress(purchase.getAddress());
            response.setPurchaseStatus(purchase.getPurchaseStatus());
            response.setStatusText(STATUS_TEXT_MAP.getOrDefault(purchase.getPurchaseStatus(), "未知"));
            response.setPurchaseType(purchase.getPurchaseType());
            response.setCreateTime(purchase.getCreateTime());
            response.setUpdateTime(purchase.getUpdateTime());

            // 只添加该农户的商品详情
            List<PurchaseDetail> orderDetails = detailMap.getOrDefault(purchase.getPurchaseId(), new ArrayList<>());
            List<OrderResponse.OrderItemResponse> items = new ArrayList<>();
            for (PurchaseDetail detail : orderDetails) {
                Product product = productMap.get(detail.getOrderId());
                // 只添加属于该农户的商品（同时匹配用户名和真实姓名）
                if (product != null && (product.getOwnName().equals(userName) || product.getOwnName().equals(user.getRealName()))) {
                    OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
                    item.setProductId(product.getOrderId());
                    item.setProductName(product.getTitle());
                    item.setProductPic(product.getPicPath());
                    item.setPrice(detail.getUninPrice());
                    item.setCount(detail.getCount());
                    item.setTotalPrice(detail.getSumPrice());
                    items.add(item);
                }
            }
            // 只有当订单中包含该农户的商品时才添加到响应
            if (!items.isEmpty()) {
                response.setItems(items);
                responses.add(response);
            }
        }

        log.info("最终返回的订单数量: {}, 总数: {}", responses.size(), purchasePage.getTotal());
        return new OrderPageResponse(responses, purchasePage.getTotal(), page, pageSize);
    }
}