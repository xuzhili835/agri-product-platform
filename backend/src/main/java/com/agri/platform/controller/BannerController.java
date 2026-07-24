package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.entity.Banner;
import com.agri.platform.service.BannerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @PostMapping
    public Result<String> add(@RequestParam String picPath,
                              @RequestParam(required = false) Integer sortOrder) {
        bannerService.addBanner(picPath, sortOrder);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{bannerId}")
    public Result<String> delete(@PathVariable Integer bannerId) {
        bannerService.deleteBanner(bannerId);
        return Result.success("删除成功");
    }

    @PutMapping("/{bannerId}")
    public Result<String> update(@PathVariable Integer bannerId,
                                  @RequestParam(required = false) String picPath,
                                  @RequestParam(required = false) Integer sortOrder) {
        bannerService.updateBanner(bannerId, picPath, sortOrder);
        return Result.success("更新成功");
    }

    @GetMapping("/all")
    public Result<List<Banner>> getAll() {
        return Result.success(bannerService.getAllBanners());
    }

    @GetMapping("/list")
    public Result<Page<Banner>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(bannerService.getBannerList(page, pageSize));
    }
}
