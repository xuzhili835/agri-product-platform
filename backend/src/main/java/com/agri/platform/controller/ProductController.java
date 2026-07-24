package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.ProductRequest;
import com.agri.platform.entity.Product;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.ProductService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @PostMapping
    public Result<String> publish(@RequestHeader("Authorization") String token,
                                   @RequestBody ProductRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        productService.publishProduct(userName, request);
        return Result.success("发布成功");
    }

    @GetMapping("/page")
    public Result<Page<Product>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(productService.getProductPage(page, pageSize, type, keyword));
    }

    @GetMapping("/{orderId}")
    public Result<Product> getById(@PathVariable Integer orderId) {
        return Result.success(productService.getProductById(orderId));
    }

    /**
     * 联系发布方：把当前用户（联系人）的姓名/电话通过站内通知发给商品/求购的发布方。
     */
    @PostMapping("/{orderId}/contact")
    public Result<String> contact(@PathVariable Integer orderId,
                                  @RequestHeader("Authorization") String token,
                                  @RequestBody(required = false) java.util.Map<String, String> body) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        String message = body == null ? null : body.get("message");
        productService.contactSeller(userName, orderId, message);
        return Result.success("已通知发布方，请保持电话畅通");
    }

    @PutMapping("/{orderId}")
    public Result<String> update(@PathVariable Integer orderId,
                                  @RequestHeader("Authorization") String token,
                                  @RequestBody ProductRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        productService.updateProduct(orderId, userName, request);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{orderId}")
    public Result<String> delete(@PathVariable Integer orderId,
                                  @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        productService.deleteProduct(orderId, userName);
        return Result.success("下架成功");
    }

    @GetMapping("/user")
    public Result<Page<Product>> myProducts(@RequestHeader("Authorization") String token,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String type) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(productService.getUserProducts(userName, page, pageSize, type));
    }

    /**
     * 批量迁移发布方名称为真实姓名（管理功能）
     */
    @PostMapping("/migrate-names")
    public Result<String> migratePublisherNames() {
        int count = productService.migratePublisherNamesToRealNames();
        return Result.success("成功迁移 " + count + " 条记录");
    }

    /**
     * 检查用户真实姓名（调试用）
     */
    @GetMapping("/check-user/{userName}")
    public Result<User> checkUser(@PathVariable String userName) {
        User user = userMapper.selectById(userName);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
}