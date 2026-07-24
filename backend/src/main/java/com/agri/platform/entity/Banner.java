package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Integer bannerId;
    private String title;
    private String picPath;
    private String linkUrl;
    private Integer sortOrder;
    private Integer status; // 1启用 0停用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}