package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.entity.Message;
import com.agri.platform.service.MessageService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息控制器：铃铛未读数、消息列表、标记已读、删除。
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/list")
    public Result<Page<Message>> list(@RequestHeader("Authorization") String token,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) Integer isRead) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        return Result.success(messageService.listMine(userName, page, pageSize, isRead));
    }

    @GetMapping("/unread/count")
    public Result<Map<String, Integer>> unreadCount(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        Map<String, Integer> data = new HashMap<>();
        data.put("count", messageService.countUnread(userName));
        return Result.success(data);
    }

    @PutMapping("/{id}/read")
    public Result<String> markRead(@PathVariable Integer id,
                                   @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        messageService.markRead(id, userName);
        return Result.success("已标记为已读");
    }

    @PutMapping("/read/all")
    public Result<String> markAllRead(@RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        messageService.markAllRead(userName);
        return Result.success("全部已读");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id,
                                 @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        messageService.delete(id, userName);
        return Result.success("已删除");
    }
}
