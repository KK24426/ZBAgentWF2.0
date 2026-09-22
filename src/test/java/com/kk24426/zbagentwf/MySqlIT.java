/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-23
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：仅在显式 mysql-it 模式经回环地址或已批准隧道验证专用测试库 CRUD 和事务。
 */
package com.kk24426.zbagentwf;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.agent.persistence.MySqlConfiguration;
import com.kk24426.zbagentwf.agent.persistence.mapper.ProbeMapper;
import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfSystemProperty(named = "mysql.it.enabled", matches = "true")
class MySqlIT {
    @Test
    void isolatedCrudAndTransactions() throws Exception {
        String url = required("ZB_TEST_DB_URL");
        String username = required("ZB_TEST_DB_USERNAME");
        String password = required("ZB_TEST_DB_PASSWORD");
        assertTrue(url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost)(?::[0-9]{1,5})?/zbagentwf_test"),
                "测试 URL 必须指向回环地址 zbagentwf_test，不能包含其它主机、库或 URL 参数。");
        SecretRedactor.register(username);
        SecretRedactor.register(password);
        String table = "zb_it_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(table.matches("zb_it_[a-f0-9]{32}"));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("mysql");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("dedicated-test", Map.of(
                    "spring.datasource.url", url, "spring.datasource.username", username,
                    "spring.datasource.password", password, "mybatis.mapper-locations", "classpath:/mapper/ProbeMapper.xml")));
            context.register(MySqlConfiguration.class, MybatisAutoConfiguration.class);
            context.refresh();
            DataSource dataSource = context.getBean(DataSource.class);
            try (Connection connection = dataSource.getConnection()) {
                assertEquals("zbagentwf_test", connection.getCatalog(), "禁止在其它 catalog 写入测试数据");
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY, name VARCHAR(255)) "
                            + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                }
                // 只有 CREATE 成功后才承担清理责任；随机碰撞或权限失败不能删除已有表。
                try {
                    ProbeMapper mapper = context.getBean(ProbeMapper.class);
                    assertEquals(1, mapper.insert(table, 1, "中文测试🙂"));
                    assertEquals("中文测试🙂", mapper.find(table, 1));
                    assertEquals(1, mapper.update(table, 1, "更新"));
                    assertEquals("更新", mapper.find(table, 1));
                    var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                    transaction.executeWithoutResult(status -> mapper.insert(table, 2, "提交"));
                    assertEquals("提交", mapper.find(table, 2));
                    assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                        mapper.insert(table, 3, "应回滚");
                        throw new IllegalStateException("rollback fixture");
                    }));
                    assertNull(mapper.find(table, 3));
                    assertEquals(1, mapper.delete(table, 1));
                    assertNull(mapper.find(table, 1));
                } finally {
                    try (var statement = connection.createStatement()) {
                        statement.executeUpdate("DROP TABLE " + table);
                    }
                }
            }
        } catch (Exception failure) {
            var text = new java.io.StringWriter();
            failure.printStackTrace(new java.io.PrintWriter(text));
            fail("MySQL 专用库验证失败：" + SecretRedactor.redact(text.toString()));
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        assertNotNull(value, "mysql-it 缺少环境变量 " + name);
        assertFalse(value.isBlank(), "mysql-it 环境变量不能为空：" + name);
        return value;
    }
}
