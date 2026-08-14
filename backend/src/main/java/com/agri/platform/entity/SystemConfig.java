package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_system_config")
public class SystemConfig {
    @TableId(type = IdType.INPUT)   // 主键(config_key)由代码指定,与 User.userName/Expert.expertName 同型
    private String configKey;
    private String configValue;
    private LocalDateTime updateTime;
}
