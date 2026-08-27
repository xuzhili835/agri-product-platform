package com.agri.platform.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单卡字段描述:写工具挂起时随确认卡下发给前端,前端渲染可编辑表单(预填模型已提取的值,
 * 缺失槽位由用户在表单里补齐)——模型不再向用户追问缺失字段。
 * <ul>
 *   <li>type: text/textarea/number/select/region/switch。region=省市区级联(前端拆 province/city/area)。</li>
 *   <li>value: 预填值(一律字符串化,数字字段由前端转回)。</li>
 *   <li>options: select 的选项(value 提交用,label 展示用)。</li>
 *   <li>hint: 字段下方说明(如"不少于15字")。</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormField {
    private String key;
    private String label;
    private String type;
    private String value;
    @Builder.Default
    private boolean required = false;
    private List<Option> options;
    private String placeholder;
    private String hint;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String value;
        private String label;
    }
}
