/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：专用测试表的 MyBatis 映射，不进入正式 JAR。
 */
package com.kk24426.zbagentwf.agent.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProbeMapper {
    int insert(@Param("table") String table, @Param("id") long id, @Param("name") String name);
    String find(@Param("table") String table, @Param("id") long id);
    int update(@Param("table") String table, @Param("id") long id, @Param("name") String name);
    int delete(@Param("table") String table, @Param("id") long id);
}
