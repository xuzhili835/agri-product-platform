package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.AddressRequest;
import com.agri.platform.entity.Address;
import com.agri.platform.service.AddressService;
import com.agri.platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<String> add(@RequestHeader("Authorization") String token,
                              @RequestBody AddressRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        addressService.addAddress(userName, request);
        return Result.success("添加成功");
    }

    @GetMapping("/list")
    public Result<List<Address>> list(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(addressService.getAddressList(userName));
    }

    @GetMapping("/default")
    public Result<Address> getDefault(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Address defaultAddress = addressService.getDefaultAddress(userName);
        return Result.success(defaultAddress);
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id,
                                  @RequestHeader("Authorization") String token,
                                  @RequestBody AddressRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        addressService.updateAddress(id, userName, request);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id,
                                  @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        addressService.deleteAddress(id, userName);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/default")
    public Result<String> setDefault(@PathVariable Integer id,
                                      @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        addressService.setDefaultAddress(id, userName);
        return Result.success("设置成功");
    }
}