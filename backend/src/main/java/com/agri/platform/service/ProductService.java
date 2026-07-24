package com.agri.platform.service;

import com.agri.platform.dto.ProductRequest;
import com.agri.platform.entity.Product;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ProductService {
    void publishProduct(String userName, ProductRequest request);
    Page<Product> getProductPage(int page, int pageSize, String type, String keyword);
    Product getProductById(Integer orderId);
    void updateProduct(Integer orderId, String userName, ProductRequest request);
    void deleteProduct(Integer orderId, String userName);
    Page<Product> getUserProducts(String userName, int page, int pageSize, String type);

    /**
     * 联系发布方：通过站内通知把联系人姓名/电话发给商品/求购的发布方。
     */
    void contactSeller(String fromUserName, Integer orderId, String message);

    /**
     * 批量迁移已发布商品的发布方名称为真实姓名
     * @return 更新的记录数
     */
    int migratePublisherNamesToRealNames();
}