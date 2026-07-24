package com.agri.platform.service;

import com.agri.platform.entity.Banner;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface BannerService {
    void addBanner(String picPath, Integer sortOrder);
    void deleteBanner(Integer bannerId);
    void updateBanner(Integer bannerId, String picPath, Integer sortOrder);
    List<Banner> getAllBanners();
    Page<Banner> getBannerList(int page, int pageSize);
}
