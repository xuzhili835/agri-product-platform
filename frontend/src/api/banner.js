/**
 * 轮播图 API 模块
 */
import request from '@/utils/request'

/**
 * 获取所有轮播图
 */
export function getBanners() {
  return request({
    url: '/banner/all',
    method: 'get'
  })
}

/**
 * 获取轮播图列表
 */
export function getBannerList() {
  return request({
    url: '/banner/list',
    method: 'get'
  })
}
