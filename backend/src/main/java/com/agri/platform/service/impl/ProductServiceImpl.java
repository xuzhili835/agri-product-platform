package com.agri.platform.service.impl;

import com.agri.platform.dto.ProductRequest;
import com.agri.platform.entity.Product;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.ProductMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.ProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    @Override
    public void publishProduct(String userName, ProductRequest request) {
        // Validate type
        if (request.getType() == null || (!request.getType().equals("goods") && !request.getType().equals("demand"))) {
            throw new RuntimeException("商品类型必须是goods或demand");
        }

        // 获取用户真实姓名
        User user = userMapper.selectById(userName);
        String publisherName = user != null && user.getRealName() != null ? user.getRealName() : userName;

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setContent(request.getContent());
        product.setPicPath(request.getPicPath());
        product.setType(request.getType());
        product.setOwnName(publisherName);
        product.setOrderStatus(0); // 默认待交易
        productMapper.insert(product);
    }

    @Override
    public Page<Product> getProductPage(int page, int pageSize, String type, String keyword) {
        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Product::getType, type);
        }
        // 关键字：模糊匹配标题或描述（市场搜索）。用 and(...) 包裹保证 OR 不污染外层条件
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getTitle, kw).or().like(Product::getContent, kw));
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        fillPhone(result.getRecords());
        return result;
    }

    @Override
    public Product getProductById(Integer orderId) {
        Product product = productMapper.selectById(orderId);
        if (product != null) {
            fillPhone(Collections.singletonList(product));
        }
        return product;
    }

    @Override
    public void updateProduct(Integer orderId, String userName, ProductRequest request) {
        Product product = productMapper.selectById(orderId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 获取用户真实姓名进行权限验证
        User user = userMapper.selectById(userName);
        String userRealName = user != null && user.getRealName() != null ? user.getRealName() : userName;

        if (!product.getOwnName().equals(userRealName)) {
            throw new RuntimeException("无权限修改此商品");
        }
        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getContent() != null) product.setContent(request.getContent());
        if (request.getPicPath() != null) product.setPicPath(request.getPicPath());
        if (request.getOrderStatus() != null) product.setOrderStatus(request.getOrderStatus());
        productMapper.updateById(product);
    }

    @Override
    public void deleteProduct(Integer orderId, String userName) {
        Product product = productMapper.selectById(orderId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 获取用户真实姓名进行权限验证
        User user = userMapper.selectById(userName);
        String userRealName = user != null && user.getRealName() != null ? user.getRealName() : userName;

        if (!product.getOwnName().equals(userRealName)) {
            throw new RuntimeException("无权限删除此商品");
        }
        productMapper.deleteById(orderId);
    }

    @Override
    public Page<Product> getUserProducts(String userName, int page, int pageSize, String type) {
        // 获取用户真实姓名进行查询
        User user = userMapper.selectById(userName);
        String userRealName = user != null && user.getRealName() != null ? user.getRealName() : userName;

        Page<Product> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getOwnName, userRealName);
        // 按类型过滤
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Product::getType, type);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productMapper.selectPage(pageParam, wrapper);
        fillPhone(result.getRecords());
        return result;
    }

    /**
     * 联系发布方：把联系人（当前用户）的姓名/电话通过站内通知发给商品/求购的发布方，
     * 便于双方线下对接；商品类由买家联系农户，求购类由农户联系发布方。
     */
    @Override
    public void contactSeller(String fromUserName, Integer orderId, String message) {
        Product product = productMapper.selectById(orderId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // 发布方 ownName 存的是真实姓名（见 publishProduct），按 真实姓名/用户名 找回发布方账号
        String ownerDisplay = product.getOwnName();
        LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
        uw.eq(User::getRealName, ownerDisplay).or().eq(User::getUserName, ownerDisplay);
        User owner = userMapper.selectList(uw).stream().findFirst().orElse(null);
        if (owner == null) {
            throw new RuntimeException("发布方不存在");
        }
        // 禁止联系自己：当前用户即发布方时拒绝
        if (owner.getUserName().equals(fromUserName)) {
            throw new RuntimeException("这是您自己发布的信息，无需联系自己");
        }

        User from = userMapper.selectById(fromUserName);
        String fromName = from != null && from.getRealName() != null ? from.getRealName() : fromUserName;
        String fromPhone = from != null ? from.getPhone() : null;
        boolean isDemand = "demand".equals(product.getType());
        String kind = isDemand ? "求购需求" : "商品";
        StringBuilder content = new StringBuilder();
        content.append(fromName).append(" 对您的").append(kind).append("「").append(product.getTitle()).append("」感兴趣，希望与您联系。");
        content.append("联系电话：").append(fromPhone != null && !fromPhone.isEmpty() ? fromPhone : "未填写");
        if (message != null && !message.trim().isEmpty()) {
            content.append("；留言：").append(message.trim());
        }
        messageService.send(owner.getUserName(), "order",
                isDemand ? "供应联系请求" : "买家联系请求",
                content.toString(),
                "/product/" + orderId);

        // 同时给联系人发一条确认，便于其留存发布方已收到
        messageService.send(fromUserName, "order",
                "已通知" + kind + "发布方",
                "已将您的联系方式通知「" + product.getTitle() + "」的发布方" + ownerDisplay + "，请保持电话畅通。",
                "/product/" + orderId);
    }

    /**
     * 批量回填发布方联系电话：ownName 可能是真实姓名或用户名，按两者匹配。
     */
    private void fillPhone(List<Product> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<String> names = list.stream()
                .map(Product::getOwnName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (names.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<User> uw = new LambdaQueryWrapper<>();
        uw.and(w -> w.in(User::getUserName, names).or().in(User::getRealName, names));
        List<User> users = userMapper.selectList(uw);
        Map<String, String> phoneByName = new HashMap<>();
        for (User u : users) {
            if (u.getPhone() == null || u.getPhone().isEmpty()) {
                continue;
            }
            if (u.getRealName() != null && names.contains(u.getRealName())) {
                phoneByName.putIfAbsent(u.getRealName(), u.getPhone());
            }
            if (u.getUserName() != null && names.contains(u.getUserName())) {
                phoneByName.putIfAbsent(u.getUserName(), u.getPhone());
            }
        }
        for (Product p : list) {
            if (p.getOwnName() != null) {
                p.setOwnPhone(phoneByName.get(p.getOwnName()));
            }
        }
    }

    /**
     * 批量迁移已发布商品的发布方名称为真实姓名
     * @return 更新的记录数
     */
    @Override
    @Transactional
    public int migratePublisherNamesToRealNames() {
        // 获取所有产品
        List<Product> allProducts = productMapper.selectList(null);
        int updateCount = 0;
        int skippedCount = 0;

        for (Product product : allProducts) {
            // 根据当前 ownName（可能是用户名或真实姓名）查找对应的用户
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUserName, product.getOwnName())
                      .or()
                      .eq(User::getRealName, product.getOwnName());
            User user = userMapper.selectOne(userWrapper);

            if (user != null && user.getRealName() != null && !user.getRealName().isEmpty()) {
                // 如果产品的 ownName 不是真实姓名，则更新
                if (!user.getRealName().equals(product.getOwnName())) {
                    product.setOwnName(user.getRealName());
                    productMapper.updateById(product);
                    updateCount++;
                    System.out.println("更新商品: " + product.getTitle() + " 发布方: " + product.getOwnName() + " -> " + user.getRealName());
                }
            } else {
                skippedCount++;
                System.out.println("跳过商品: " + product.getTitle() + " 发布方: " + product.getOwnName() + " (用户不存在或无真实姓名)");
            }
        }

        System.out.println("迁移完成: 更新 " + updateCount + " 条，跳过 " + skippedCount + " 条");
        return updateCount;
    }
}