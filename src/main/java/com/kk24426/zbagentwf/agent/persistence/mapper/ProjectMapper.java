/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：提供项目四表的参数绑定SQL入口，不定义业务事务。
 */
package com.kk24426.zbagentwf.agent.persistence.mapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/** 只映射技术行；聚合完整性、提交后发布和事务由ProjectDao负责。 */
@Mapper
public interface ProjectMapper {
    /** 插入Agent行并只回填技术行的自增ID；实体提交后才发布。 */
    int insertAgent(Map<String, Object> row);
    /** 按主键和版本更新Agent；0行表示冲突或不存在。 */
    int updateAgent(Map<String, Object> row);
    /** 读取未逻辑删除的Agent行；调用方提供一致读事务。 */
    Map<String, Object> getAgent(@Param("id") Long id);
    /** 插入Project行并只回填技术行的自增ID；实体提交后才发布。 */
    int insertProject(Map<String, Object> row);
    /** 按主键和版本更新Project；0行表示冲突或不存在。 */
    int updateProject(Map<String, Object> row);
    /** 读取未逻辑删除的Project行；调用方提供一致读事务。 */
    Map<String, Object> getProject(@Param("id") Long id);
    /** 插入Requirement行并只回填技术行的自增ID；实体提交后才发布。 */
    int insertRequirement(Map<String, Object> row);
    /** 按主键和版本更新Requirement；0行表示冲突或不存在。 */
    int updateRequirement(Map<String, Object> row);
    /** 读取未逻辑删除的Requirement行；调用方提供一致读事务。 */
    Map<String, Object> getRequirement(@Param("id") Long id);
    /** 按已保存顺序读取父实体的子行；不包含逻辑删除项。 */
    List<Map<String, Object>> listRequirements(@Param("parentId") Long parentId);
    /** 保存时读取含逻辑删除项的所有需求行，保留排序槽并核对归属。 */
    List<Map<String,Object>> listStoredRequirements(@Param("parentId") Long parentId);
    /** 插入Task行并只回填技术行的自增ID；实体提交后才发布。 */
    int insertTask(Map<String, Object> row);
    /** 按主键和版本更新Task；0行表示冲突或不存在。 */
    int updateTask(Map<String, Object> row);
    /** 读取未逻辑删除的Task行；调用方提供一致读事务。 */
    Map<String, Object> getTask(@Param("id") Long id);
    /** 按已保存顺序读取父实体的子行；不包含逻辑删除项。 */
    List<Map<String, Object>> listTasks(@Param("parentId") Long parentId);
    /** 保存时读取含逻辑删除项的所有任务行，不用于业务查询展示。 */
    List<Map<String,Object>> listStoredTasks(@Param("parentId") Long parentId);
    /** 将业务UUID转换为项目数据库主键。 */
    Long findProjectId(@Param("projectId") String projectId);
    /** 读取未删除项目主键，供聚合列表加载。 */
    List<Long> projectIds();
    /** 轻量检查四张项目表可访问，不创建表或修改数据。 */
    int checkSchema();
}
