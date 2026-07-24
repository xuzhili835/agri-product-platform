package com.agri.platform.service.impl;

import com.agri.platform.entity.Banner;
import com.agri.platform.mapper.BannerMapper;
import com.agri.platform.service.BannerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public void addBanner(String picPath, Integer sortOrder) {
        Banner banner = new Banner();
        banner.setPicPath(picPath);
        banner.setSortOrder(sortOrder != null ? sortOrder : 0);
        bannerMapper.insert(banner);
    }

    @Override
    public void deleteBanner(Integer bannerId) {
        bannerMapper.deleteById(bannerId);
    }

    @Override
    public void updateBanner(Integer bannerId, String picPath, Integer sortOrder) {
        Banner banner = bannerMapper.selectById(bannerId);
        if (banner == null) {
            throw new RuntimeException("轮播图不存在");
        }
        if (picPath != null) banner.setPicPath(picPath);
        if (sortOrder != null) banner.setSortOrder(sortOrder);
        bannerMapper.updateById(banner);
    }

    @Override
    public List<Banner> getAllBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder);
        return bannerMapper.selectList(wrapper);
    }

    @Override
    public Page<Banner> getBannerList(int page, int pageSize) {
        Page<Banner> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder);
        return bannerMapper.selectPage(pageParam, wrapper);
    }
}
