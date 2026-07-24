package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.ExpertRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.Reserve;
import com.agri.platform.service.ExpertService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expert")
public class ExpertController {

    @Autowired
    private ExpertService expertService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/my/info")
    public Result<Expert> getInfo(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(expertService.getExpertByUserName(userName));
    }

    @PutMapping("/my/info")
    public Result<String> updateInfo(@RequestHeader("Authorization") String token,
                                     @RequestBody ExpertRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        expertService.updateExpert(userName, request);
        return Result.success("更新成功");
    }

    @GetMapping("/reserve/list")
    public Result<Page<Reserve>> reserveList(@RequestHeader("Authorization") String token,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Integer status) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Expert expert = expertService.getExpertByUserName(userName);
        if (expert == null) {
            return Result.error("专家信息不存在");
        }
        // expert_name 存储的是专家 userName
        return Result.success(expertService.getReserveList(expert.getUserName(), page, pageSize, status));
    }

    @PutMapping("/reserve/{reserveId}/status")
    public Result<String> updateReserveStatus(@PathVariable Integer reserveId,
                                              @RequestHeader("Authorization") String token,
                                              @RequestBody java.util.Map<String, Object> payload) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Expert expert = expertService.getExpertByUserName(userName);
        if (expert == null) {
            return Result.error("专家信息不存在");
        }
        Integer status = payload.get("status") != null ? Integer.valueOf(payload.get("status").toString()) : 0;
        String answer = payload.get("answer") != null ? payload.get("answer").toString() : null;
        expertService.confirmReserve(reserveId, expert.getUserName(), status, answer);
        return Result.success("状态更新成功");
    }

    @GetMapping("/list")
    public Result<List<Expert>> list() {
        return Result.success(expertService.getAllExperts());
    }

    /**
     * 获取专家详情（公共接口）
     */
    @GetMapping("/{userName}")
    public Result<Expert> getExpertDetail(@PathVariable String userName) {
        Expert expert = expertService.getExpertByUserName(userName);
        if (expert == null) {
            return Result.error("专家不存在");
        }
        return Result.success(expert);
    }
}
