package com.agri.platform.service.impl;

import com.agri.platform.dto.KnowledgeRequest;
import com.agri.platform.entity.Discuss;
import com.agri.platform.entity.Knowledge;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.DiscussMapper;
import com.agri.platform.mapper.KnowledgeMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.KnowledgeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private DiscussMapper discussMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void publishKnowledge(String userName, KnowledgeRequest request) {
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(request.getTitle());
        knowledge.setContent(request.getContent());
        knowledge.setPicPath(request.getPicPath());
        knowledge.setOwnName(userName);
        // 默认发布（1）；草稿需显式传 0
        knowledge.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        knowledgeMapper.insert(knowledge);
    }

    @Override
    public Page<Knowledge> getKnowledgeList(int page, int pageSize) {
        return getKnowledgeList(page, pageSize, 1, null);
    }

    @Override
    public Page<Knowledge> getKnowledgeList(int page, int pageSize, Integer status, String ownName) {
        Page<Knowledge> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Knowledge::getStatus, status);
        } else if (ownName == null || ownName.isEmpty()) {
            // 公共列表（未指定发布者）只展示已发布，避免泄露他人草稿
            wrapper.eq(Knowledge::getStatus, 1);
        }
        if (ownName != null && !ownName.isEmpty()) {
            wrapper.eq(Knowledge::getOwnName, ownName);
        }
        wrapper.orderByDesc(Knowledge::getCreateTime);
        Page<Knowledge> result = knowledgeMapper.selectPage(pageParam, wrapper);
        fillOwnRealName(result.getRecords());
        fillCommentCount(result.getRecords());
        return result;
    }

    @Override
    public Knowledge getKnowledgeById(Integer knowledgeId) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge != null) {
            fillOwnRealName(knowledge);
            fillCommentCount(Collections.singletonList(knowledge));
        }
        return knowledge;
    }

    @Override
    public void updateKnowledge(Integer knowledgeId, String userName, KnowledgeRequest request) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        if (!knowledge.getOwnName().equals(userName) && !isAdmin(userName)) {
            throw new RuntimeException("无权限修改此知识");
        }
        if (request.getTitle() != null) knowledge.setTitle(request.getTitle());
        if (request.getContent() != null) knowledge.setContent(request.getContent());
        if (request.getPicPath() != null) knowledge.setPicPath(request.getPicPath());
        if (request.getStatus() != null) knowledge.setStatus(request.getStatus());
        knowledgeMapper.updateById(knowledge);
    }

    @Override
    public void deleteKnowledge(Integer knowledgeId, String userName) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new RuntimeException("知识不存在");
        }
        if (!knowledge.getOwnName().equals(userName) && !isAdmin(userName)) {
            throw new RuntimeException("无权限删除此知识");
        }
        knowledgeMapper.deleteById(knowledgeId);
    }

    /** 判断当前用户是否为管理员（管理员可管理所有知识文章，用于内容审核） */
    private boolean isAdmin(String userName) {
        User u = userMapper.selectById(userName);
        return u != null && "admin".equals(u.getRole());
    }

    @Override
    public void addDiscuss(Integer knowledgeId, String userName, String content) {
        Discuss discuss = new Discuss();
        discuss.setKnowledgeId(knowledgeId);
        discuss.setOwnName(userName);
        discuss.setContent(content);
        discussMapper.insert(discuss);
    }

    @Override
    public List<Discuss> getDiscussList(Integer knowledgeId) {
        List<Discuss> list = discussMapper.selectList(
            new LambdaQueryWrapper<Discuss>()
                .eq(Discuss::getKnowledgeId, knowledgeId)
                .orderByAsc(Discuss::getCreateTime)
        );
        fillDiscussRealName(list);
        return list;
    }

    @Override
    public void deleteDiscuss(Integer discussId, String userName) {
        Discuss discuss = discussMapper.selectById(discussId);
        if (discuss == null) {
            throw new RuntimeException("评论不存在");
        }
        // 评论者本人或知识发布者可以删除
        if (!discuss.getOwnName().equals(userName)) {
            // 检查是否是知识发布者
            Knowledge knowledge = knowledgeMapper.selectById(discuss.getKnowledgeId());
            if (knowledge == null || !knowledge.getOwnName().equals(userName)) {
                throw new RuntimeException("无权限删除此评论");
            }
        }
        discussMapper.deleteById(discussId);
    }

    // ===== realName 回填（ownRealName 为非数据库列，仅用于展示真实姓名） =====

    /** 批量回填知识列表发布者真实姓名 */
    private void fillOwnRealName(List<Knowledge> list) {
        if (list == null || list.isEmpty()) return;
        Map<String, String> nameMap = mapRealNames(
            list.stream().map(Knowledge::getOwnName).collect(Collectors.toList())
        );
        for (Knowledge k : list) {
            k.setOwnRealName(nameMap.getOrDefault(k.getOwnName(), k.getOwnName()));
        }
    }

    /** 单条知识发布者真实姓名 */
    private void fillOwnRealName(Knowledge k) {
        if (k == null) return;
        Map<String, String> nameMap = mapRealNames(Collections.singletonList(k.getOwnName()));
        k.setOwnRealName(nameMap.getOrDefault(k.getOwnName(), k.getOwnName()));
    }

    /** 批量回填评论数：一次查出这些知识的全部 tb_discuss，按 knowledgeId 分组计数 */
    private void fillCommentCount(List<Knowledge> list) {
        if (list == null || list.isEmpty()) return;
        List<Integer> ids = list.stream()
            .map(Knowledge::getKnowledgeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<Discuss> discusses = discussMapper.selectList(
            new LambdaQueryWrapper<Discuss>().in(Discuss::getKnowledgeId, ids)
        );
        Map<Integer, Long> countMap = discusses.stream()
            .collect(Collectors.groupingBy(Discuss::getKnowledgeId, Collectors.counting()));
        for (Knowledge k : list) {
            k.setCommentCount(countMap.getOrDefault(k.getKnowledgeId(), 0L).intValue());
        }
    }

    /** 批量回填评论人真实姓名 */
    private void fillDiscussRealName(List<Discuss> list) {
        if (list == null || list.isEmpty()) return;
        Map<String, String> nameMap = mapRealNames(
            list.stream().map(Discuss::getOwnName).collect(Collectors.toList())
        );
        for (Discuss d : list) {
            d.setOwnRealName(nameMap.getOrDefault(d.getOwnName(), d.getOwnName()));
        }
    }

    /** 按 userName 集合批量查 tb_user.real_name，返回 userName→realName（realName 为空的不会进入，由调用方兜底 ownName） */
    private Map<String, String> mapRealNames(Collection<String> userNames) {
        if (userNames == null) return Collections.emptyMap();
        List<String> names = userNames.stream()
            .filter(Objects::nonNull)
            .filter(n -> !n.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        if (names.isEmpty()) return Collections.emptyMap();
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getUserName, names)
        );
        Map<String, String> result = new HashMap<>();
        for (User u : users) {
            if (u.getRealName() != null && !u.getRealName().isEmpty()) {
                result.put(u.getUserName(), u.getRealName());
            }
        }
        return result;
    }
}
