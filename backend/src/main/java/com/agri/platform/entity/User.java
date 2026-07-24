package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.INPUT)
    private String userName;

    private String password;

    private String realName;

    private String phone;

    private String identityNum;

    private String address;

    private String role;  // farmer, expert, bank, admin

    private String avatar;

    private Integer integral;

    private Integer credit;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
