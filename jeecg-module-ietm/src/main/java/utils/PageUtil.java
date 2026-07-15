package utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collections;
import java.util.List;

/**
 * @author: zxk
 * @description: list转换为page工具类
 * @date: 2026/1/30 16:33
 * @param:
 * @return:
 * @version: 1.0
 */
public class PageUtil {
    public static <T> Page<T> getPageResult(List<T> list, Integer pageNo, Integer pageSize) {
        Page<T> page = new Page<>(pageNo, pageSize);
        int total = list.size();
        page.setTotal(total);
        int start = (pageNo - 1) * pageSize;
        int end = Integer.min(start + pageSize, total);
        if (start < total && start >= 0) {
            page.setRecords(list.subList(start, end));
        } else {
            page.setRecords(Collections.emptyList());
        }
        return page;
    }
}
