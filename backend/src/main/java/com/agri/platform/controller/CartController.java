package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.CartRequest;
import com.agri.platform.dto.CartResponse;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.CartService;
import com.agri.platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @PostMapping
    public Result<String> add(@RequestHeader("Authorization") String token,
                              @RequestBody CartRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        // 仅买家可加入购物车（农户等角色不可）
        User u = userMapper.selectById(userName);
        if (u == null || !"buyer".equals(u.getRole())) {
            return Result.error("仅买家账号可加入购物车");
        }
        cartService.addToCart(userName, request);
        return Result.success("加入购物车成功");
    }

    @GetMapping("/list")
    public Result<List<CartResponse>> list(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(cartService.getCartList(userName));
    }

    @PutMapping("/{shoppingId}")
    public Result<String> update(@PathVariable Integer shoppingId,
                                  @RequestHeader("Authorization") String token,
                                  @RequestBody Map<String, Integer> request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Integer count = request.get("count");
        cartService.updateCartCount(shoppingId, userName, count);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{shoppingId}")
    public Result<String> delete(@PathVariable Integer shoppingId,
                                  @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        cartService.deleteFromCart(shoppingId, userName);
        return Result.success("删除成功");
    }
}