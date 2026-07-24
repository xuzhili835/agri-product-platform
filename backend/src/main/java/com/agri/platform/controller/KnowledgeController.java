package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.DiscussRequest;
import com.agri.platform.dto.KnowledgeRequest;
import com.agri.platform.entity.Discuss;
import com.agri.platform.entity.Knowledge;
import com.agri.platform.service.KnowledgeService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/list")
    public Result<Page<Knowledge>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String ownName) {
        return Result.success(knowledgeService.getKnowledgeList(page, pageSize, status, ownName));
    }

    @GetMapping("/{knowledgeId}")
    public Result<Knowledge> getById(@PathVariable Integer knowledgeId) {
        return Result.success(knowledgeService.getKnowledgeById(knowledgeId));
    }

    @PostMapping
    public Result<String> publish(@RequestHeader("Authorization") String token,
                                   @RequestBody KnowledgeRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        knowledgeService.publishKnowledge(userName, request);
        return Result.success("发布成功");
    }

    @PutMapping("/{knowledgeId}")
    public Result<String> update(@PathVariable Integer knowledgeId,
                                  @RequestHeader("Authorization") String token,
                                  @RequestBody KnowledgeRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        knowledgeService.updateKnowledge(knowledgeId, userName, request);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{knowledgeId}")
    public Result<String> delete(@PathVariable Integer knowledgeId,
                                  @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        knowledgeService.deleteKnowledge(knowledgeId, userName);
        return Result.success("删除成功");
    }

    // 评论相关
    @PostMapping("/{knowledgeId}/discuss")
    public Result<String> addDiscuss(@PathVariable Integer knowledgeId,
                                      @RequestHeader("Authorization") String token,
                                      @RequestBody DiscussRequest request) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        knowledgeService.addDiscuss(knowledgeId, userName, request.getContent());
        return Result.success("评论成功");
    }

    @GetMapping("/{knowledgeId}/discuss/list")
    public Result<List<Discuss>> discussList(@PathVariable Integer knowledgeId) {
        return Result.success(knowledgeService.getDiscussList(knowledgeId));
    }

    @DeleteMapping("/discuss/{discussId}")
    public Result<String> deleteDiscuss(@PathVariable Integer discussId,
                                         @RequestHeader("Authorization") String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        knowledgeService.deleteDiscuss(discussId, userName);
        return Result.success("删除成功");
    }
}
