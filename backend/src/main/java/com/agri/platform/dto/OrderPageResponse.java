package com.agri.platform.dto;

import lombok.Data;
import java.util.List;

/**
 * 订单分页响应
 */
@Data
public class OrderPageResponse {
    private List<OrderResponse> records;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public OrderPageResponse(List<OrderResponse> records, Long total, Integer page, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}
