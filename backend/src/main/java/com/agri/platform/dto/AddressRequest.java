package com.agri.platform.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private Integer id;
    private String consignee;
    private String phone;
    private String province;
    private String city;
    private String area;
    private String addressDetail;
    private Integer isDefault;
}