package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.entity.JointInvitation;
import com.agri.platform.service.JointInvitationService;
import com.agri.platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联合贷款人邀请：被邀请人查看/同意/拒绝，农户查看某笔申请下的邀请状态。
 */
@RestController
@RequestMapping("/joint-invitation")
public class JointInvitationController {

    @Autowired
    private JointInvitationService jointInvitationService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 我收到的联合贷款邀请 */
    @GetMapping("/mine")
    public Result<List<JointInvitation>> mine(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(jointInvitationService.listMine(userName));
    }

    /** 某笔融资申请下的全部邀请（按 slot 排序），用于农户查看各联合人确认状态 */
    @GetMapping("/finance/{financeId}")
    public Result<List<JointInvitation>> byFinance(@PathVariable Integer financeId) {
        return Result.success(jointInvitationService.listByFinance(financeId));
    }

    /** 同意邀请 */
    @PostMapping("/{id}/accept")
    public Result<String> accept(@RequestHeader("Authorization") String token,
                                 @PathVariable Integer id) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        jointInvitationService.accept(id, userName);
        return Result.success("已同意");
    }

    /** 拒绝邀请 */
    @PostMapping("/{id}/decline")
    public Result<String> decline(@RequestHeader("Authorization") String token,
                                  @PathVariable Integer id) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        jointInvitationService.decline(id, userName);
        return Result.success("已拒绝");
    }
}
