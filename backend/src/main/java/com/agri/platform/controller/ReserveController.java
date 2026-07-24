package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.ReserveRequest;
import com.agri.platform.entity.Reserve;
import com.agri.platform.service.ReserveService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reserve")
public class ReserveController {

    @Autowired
    private ReserveService reserveService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<String> make(@RequestHeader("Authorization") String token,
                               @RequestBody ReserveRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        reserveService.makeReservation(userName, request);
        return Result.success("预约成功");
    }

    @GetMapping("/my")
    public Result<Page<Reserve>> myReserves(@RequestHeader("Authorization") String token,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(reserveService.getReserveList(userName, page, pageSize));
    }

    @DeleteMapping("/{reserveId}")
    public Result<String> cancel(@PathVariable Integer reserveId,
                                  @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        reserveService.cancelReserve(reserveId, userName);
        return Result.success("取消成功");
    }
}
