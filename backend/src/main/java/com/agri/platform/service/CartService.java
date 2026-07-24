package com.agri.platform.service;

import com.agri.platform.dto.CartRequest;
import com.agri.platform.dto.CartResponse;
import com.agri.platform.entity.ShoppingCart;
import java.util.List;

public interface CartService {
    void addToCart(String userName, CartRequest request);
    List<CartResponse> getCartList(String userName);
    void updateCartCount(Integer shoppingId, String userName, Integer count);
    void deleteFromCart(Integer shoppingId, String userName);
}