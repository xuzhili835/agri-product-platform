package com.agri.platform.service;

import com.agri.platform.dto.RoleApplicationRequest;
import com.agri.platform.entity.RoleApplication;

import java.util.List;

public interface RoleApplicationService {

    /** 农户/买家提交角色申请 */
    void apply(String userName, RoleApplicationRequest request);

    /** 查看我的申请记录 */
    List<RoleApplication> listMine(String userName);

    /** 管理员查看全部申请（status 为 null 时查全部） */
    List<RoleApplication> listAll(Integer status);

    /** 管理员审核（status: 1通过 2驳回） */
    void review(Integer id, String reviewer, Integer status, String reviewRemark);
}
