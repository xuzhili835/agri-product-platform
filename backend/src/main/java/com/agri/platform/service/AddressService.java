package com.agri.platform.service;

import com.agri.platform.dto.AddressRequest;
import com.agri.platform.entity.Address;
import java.util.List;

public interface AddressService {
    void addAddress(String userName, AddressRequest request);
    List<Address> getAddressList(String userName);
    Address getDefaultAddress(String userName);
    void updateAddress(Integer id, String userName, AddressRequest request);
    void deleteAddress(Integer id, String userName);
    void setDefaultAddress(Integer id, String userName);
}