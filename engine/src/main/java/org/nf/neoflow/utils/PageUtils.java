package org.nf.neoflow.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页工具
 * @author PC8650
 */
public class PageUtils {

    /**
     * 初始化分页
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @param orderBy 排序字段
     * @param desc 是否降序
     * @return Pageable
     */
    public static Pageable initPageable(Integer pageNumber, Integer pageSize, String orderBy, Boolean desc) {
        Sort sort;
        if (desc) {
            sort = Sort.by(Sort.Direction.DESC,orderBy);
        }else {
            sort = Sort.by(Sort.Direction.ASC,orderBy);
        }

        return PageRequest.of(pageNumber - 1, pageSize, sort);
    }

}
