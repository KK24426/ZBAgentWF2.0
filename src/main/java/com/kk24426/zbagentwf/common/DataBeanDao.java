/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：定义用户提供的数据对象查询及保存接口。
 */
package com.kk24426.zbagentwf.common;

import java.util.List;

/** 通用数据访问签名；具体实现声明聚合范围和事务语义，接口本身不发起SQL。 */
public interface DataBeanDao<T extends DataBean> {
    /** 查询可见实体；返回值是否为快照由实现规定。 */
    public List<T> selectAll();
    /** 按数据库Long主键读取；不存在返回null。 */
    public T selectById(Long Id);
    /** 保存既有实体，返回更新实体件数；版本冲突不能伪装成功。 */
    public int update(T t);
    /** 保存多实体，返回更新实体件数；原子性由具体DAO声明。 */
    public int update(List<T> list);
    /** 插入新实体，成功后回填生成的主键，返回插入实体件数。 */
    public int insert(T t);
    /** 插入多个新实体并回填主键，返回插入实体件数；原子性由具体DAO声明。 */
    public int insertAll(List<T> list);
}
