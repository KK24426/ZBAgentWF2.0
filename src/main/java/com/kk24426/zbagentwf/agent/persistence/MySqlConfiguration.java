/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：仅在 mysql profile 下配置连接池、Mapper 扫描和事务。
 */
package com.kk24426.zbagentwf.agent.persistence;

import com.kk24426.zbagentwf.common.logging.SecretRedactor;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** 显式启用 mysql 才建立连接，不自动建库、建表或迁移。 */
@Configuration(proxyBeanMethods = false)
@Profile("mysql")
@EnableConfigurationProperties(DataSourceProperties.class)
@EnableTransactionManagement
@MapperScan(basePackages = "com.kk24426.zbagentwf.agent.persistence.mapper", annotationClass = Mapper.class)
public class MySqlConfiguration {
    /**
     * 仅在mysql profile下验证必要连接配置并建立受限连接池对象，登记账号/密码供脱敏；连接验证在后续初始化阶段进行。
     */
    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(DataSourceProperties properties) {
        if (properties.getUrl() == null || !properties.getUrl().startsWith("jdbc:mysql://")
                || properties.getUsername() == null || properties.getUsername().isBlank()
                || properties.getPassword() == null || properties.getPassword().isBlank()) {
            throw new IllegalStateException("mysql profile 需要有效的 URL、用户名和密码配置。");
        }
        SecretRedactor.register(properties.getUsername());
        SecretRedactor.register(properties.getPassword());
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        dataSource.setPoolName("zbagentwf-mysql");
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(5000);
        dataSource.setValidationTimeout(3000);
        dataSource.addDataSourceProperty("connectTimeout", "5000");
        dataSource.addDataSourceProperty("socketTimeout", "30000");
        return dataSource;
    }

    /**
     * 提供启动阶段的实际连接检查，使用后归还连接；连接不可用即令初始化失败，不降级其它数据库。
     */
    @Bean
    InitializingBean verifyDatabaseConnection(DataSource dataSource) {
        return () -> {
            try (var connection = dataSource.getConnection()) {
                if (!connection.isValid(3)) {
                    throw new IllegalStateException("MySQL 连接验证失败。");
                }
            }
        };
    }

    /**
     * 为同一数据源提供Spring事务管理器；这里不声明业务事务范围，也不执行SQL。
     */
    @Bean
    JdbcTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * 启用下划线转驼峰并关闭原生SQL/参数/结果日志；执行元数据由专用诊断拦截器记录。
     */
    @Bean
    ConfigurationCustomizer mybatisConfiguration() {
        return configuration -> {
            configuration.setMapUnderscoreToCamelCase(true);
            // 禁止原生 SQL/参数/结果日志；由诊断拦截器记录无参数的执行元数据。
            configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        };
    }

    @Bean
    SqlDiagnosticsInterceptor sqlDiagnosticsInterceptor() {
        return new SqlDiagnosticsInterceptor();
    }
}
