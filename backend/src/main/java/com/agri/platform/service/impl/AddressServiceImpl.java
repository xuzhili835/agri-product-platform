package com.agri.platform.service.impl;

import com.agri.platform.dto.AddressRequest;
import com.agri.platform.entity.Address;
import com.agri.platform.mapper.AddressMapper;
import com.agri.platform.service.AddressService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressMapper addressMapper;

    @Override
    public void addAddress(String userName, AddressRequest request) {
        Long count = addressMapper.selectCount(
            new LambdaQueryWrapper<Address>().eq(Address::getOwnName, userName)
        );
        // 第一个地址自动默认；用户明确设默认也置为默认
        boolean willBeDefault = count == 0 || (request.getIsDefault() != null && request.getIsDefault() == 1);

        // 设为默认前先取消既有默认，避免出现多条 isDefault=1 共存（与 updateAddress 行为一致）
        if (willBeDefault && count > 0) {
            addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                    .eq(Address::getOwnName, userName)
                    .set(Address::getIsDefault, 0));
        }

        Address address = new Address();
        address.setOwnName(userName);
        address.setConsignee(request.getConsignee());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setArea(request.getArea());
        address.setAddressDetail(request.getAddressDetail());
        address.setIsDefault(willBeDefault ? 1 : 0);
        addressMapper.insert(address);
    }

    @Override
    public List<Address> getAddressList(String userName) {
        return addressMapper.selectList(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getOwnName, userName)
                .orderByDesc(Address::getIsDefault)
        );
    }

    @Override
    public Address getDefaultAddress(String userName) {
        return addressMapper.selectOne(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getOwnName, userName)
                .eq(Address::getIsDefault, 1)
                .last("LIMIT 1")
        );
    }

    @Override
    public void updateAddress(Integer id, String userName, AddressRequest request) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getOwnName().equals(userName)) {
            throw new RuntimeException("地址不存在或无权限");
        }
        if (request.getConsignee() != null) address.setConsignee(request.getConsignee());
        if (request.getPhone() != null) address.setPhone(request.getPhone());
        if (request.getProvince() != null) address.setProvince(request.getProvince());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getArea() != null) address.setArea(request.getArea());
        if (request.getAddressDetail() != null) address.setAddressDetail(request.getAddressDetail());
        if (request.getIsDefault() != null) {
            // 如果设为默认地址，需要先取消其他默认地址
            if (request.getIsDefault() == 1) {
                addressMapper.update(null,
                    new LambdaUpdateWrapper<Address>()
                        .eq(Address::getOwnName, userName)
                        .set(Address::getIsDefault, 0));
            }
            address.setIsDefault(request.getIsDefault());
        }
        addressMapper.updateById(address);
    }

    @Override
    public void deleteAddress(Integer id, String userName) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getOwnName().equals(userName)) {
            throw new RuntimeException("地址不存在或无权限");
        }
        addressMapper.deleteById(id);
        // 若删除的恰是默认地址，把剩余最近的一条提升为默认，避免库内"无默认"导致结算取不到默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            Address latest = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getOwnName, userName)
                    .orderByDesc(Address::getId)
                    .last("LIMIT 1")
            );
            if (latest != null) {
                latest.setIsDefault(1);
                addressMapper.updateById(latest);
            }
        }
    }

    @Override
    public void setDefaultAddress(Integer id, String userName) {
        // 先校验归属：原实现先清空当前用户所有默认、再校验 id，
        // 一旦传入不存在/他人的 id 就会把该用户全部默认地址静默清零。改为先校验、再清、再置。
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getOwnName().equals(userName)) {
            throw new RuntimeException("地址不存在或无权限");
        }
        // 取消当前用户的其他默认地址
        addressMapper.update(null,
            new LambdaUpdateWrapper<Address>()
                .eq(Address::getOwnName, userName)
                .set(Address::getIsDefault, 0));
        // 置为新默认
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }
}