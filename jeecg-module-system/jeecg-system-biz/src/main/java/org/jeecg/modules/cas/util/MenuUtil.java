package org.jeecg.modules.cas.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @ClassName MenuUtil
 * @Description
 * @Author CMZ
 * @Date 2023/8/14 10:42
 * @Version 1.0
 */
public class MenuUtil {
    @Autowired
    private static JdbcTemplate jdbcTemplate;

    public static void changeToVue2Menu() {
        jdbcTemplate.execute("alter table sys_permission rename TO sys_psermission_v3");
        jdbcTemplate.execute("alter table sys_permission_v2 rename TO sys_permission");
    }
}
