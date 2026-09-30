# 项目结构、变更计划与履历骨架

2026-09-30 用户批准先编写实体与抽象 Domain，审阅后再实现业务。本轮新增类型位于现有
`common.project.model` 和 `user.project.api`，没有新增包职责或 Maven 模块。

**交付状态：仅数据类型和抽象契约。** 未建立新数据库表、DAO、Mapper、SQL、Spring 实现、
HTTP 接口或页面，也未将这些数据接入现有需求规划、任务执行和 Agent 上下文。
本文“必须”“拒绝”“原子”“幂等”等业务行为是后续实现要求，本轮没有相应运行能力。

## 结构与身份

```text
Project
  └─ ProjectApplication（后台管理、网页前台、App、API 服务等）
       └─ ProjectModule（订单等单层模块）
            └─ ProjectFunction（新增订单、修改订单等具体功能）
```

同名模块或功能在不同应用内是不同对象。例如后台“修改订单”和前台“修改订单”分别保存，
不通过名称共享身份。页面、按钮和后台服务可写入功能描述及实现引用，本轮不另加结构层级。
当前结构描述已经存在的系统；新增计划不会提前创建当前态。

六个实体均继承 `DataBean` 的 `id/creationData/lastupdateData/delFlg/version`，不重复时间字段。
普通 Bean 原样存取，不自动生成 ID、验证业务或访问数据库。关联全部使用数据库 Long 主键：

| 目标实体 | 外键 |
| --- | --- |
| Project | projectId |
| ProjectApplication | projectApplicationId |
| ProjectModule | projectModuleId |
| ProjectFunction | projectFunctionId |
| Requirement | requirementId |
| RequirementTask | requirementTaskId |
| RequirementChangePlanItem | requirementChangePlanItemId |

新类型及 Domain 参数中的 `projectId: Long` 指 `Project.id`。
现有 `Project.projectId: String` 仍是业务 UUID；原有 ProjectDomain 和 HTTP 契约不变。
`executionId` 是单次任务执行 UUID；`itemKey/parentItemKey` 是规划引用键，均不是数据库外键。

## 实体

| 类型 | 本轮新增字段 |
| --- | --- |
| ProjectApplication | projectId、name、description |
| ProjectModule | projectApplicationId、name、description |
| ProjectFunction | projectModuleId、name、summary、description、implementationRefs |
| RequirementChangePlanItem | projectId、requirementId、itemKey、targetType、operation、projectApplicationId、projectModuleId、projectFunctionId、parentItemKey、targetName、changeDescription、unchangedBehavior、acceptanceCriteria |
| TaskChangePlanLink | projectId、requirementId、requirementTaskId、requirementChangePlanItemId |
| ProjectChangeRecord | projectId、requirementId、requirementTaskId、requirementChangePlanItemId、executionId、targetType、operation、projectApplicationId、projectModuleId、projectFunctionId、changeDescription、beforeSnapshot、afterSnapshot |

功能的 summary 用于简短检索，description 记录详细当前行为；
implementationRefs 为可空的代码位置、页面路径或 API 引用列表，不复制完整实现或 API schema，
也不会自动读取或执行引用内容。

`ProjectElementType` 只有 APPLICATION、MODULE、FUNCTION。
`ProjectChangeOperation` 只有 CREATE、UPDATE；本轮没有删除、停用、移动或递归模块。

## 计划与任务边界

计划统一使用显式归属路径，不使用 targetId/parentId：

| targetType | 目标路径 | 必须为空的下级字段 |
| --- | --- | --- |
| APPLICATION | projectApplicationId | projectModuleId、projectFunctionId |
| MODULE | projectApplicationId → projectModuleId | projectFunctionId |
| FUNCTION | projectApplicationId → projectModuleId → projectFunctionId | 无 |

CREATE 的目标 ID 为空；已有祖先填写实际 ID。直接父对象也尚未创建时，parentItemKey
引用同一需求中相邻父层级的 CREATE 计划，可引用本批或此前已保存计划；
尚未生成的祖先 ID 可空，已填写的祖先不得与解析路径冲突。
UPDATE 必须提供完整已有路径，parentItemKey 为空，不能移动父级。
itemKey 由编排方提供，在同一需求中唯一；Bean 构造器不会生成它。

一个任务可以修改同一模块自身及多个功能，不能跨模块或跨应用。
模块尚未创建时通过其 CREATE 计划 itemKey 区分身份，不能按 null 主键合并边界。
应用级创建或修改由独立任务负责，不混入模块/功能计划。
任务关联只能为已有 PENDING 任务整体设置，设置关联不推进状态。
changeDescription、unchangedBehavior 和 acceptanceCriteria 分别约束修改范围、保持不变的行为和验收标准。

## 强类型快照与实际履历

`ProjectElementSnapshot` 是 sealed interface，仅允许三个不可变嵌套 record：
ApplicationSnapshot、ModuleSnapshot、FunctionSnapshot。
各自保存对应业务字段、Long id、直接父级外键、Integer version 和 boolean delFlg。
新增候选可暂缺尚未生成的 ID、直接父级 ID 和版本；保存后的历史必须完整。
Integer version 的 null 表示候选尚未生成，不等于数据库版本 0。

FunctionSnapshot 防御复制非空 implementationRefs，返回不可修改的列表；
null 保留为 null，空列表保持为空，含 null 元素的列表在构造时抛 NullPointerException。
其余数据都是不可变值，不持有可变实体引用；构造不做业务归属校验或外部调用。

`ProjectChangeSubmission` 包含 requirementChangePlanItemId、beforeSnapshot、
afterSnapshot、changeDescription，表达已核实实际修改的候选。
发布方负责验证候选；记录履历不等于执行代码或判定整个任务验收成功。

后续 `publishTaskChanges` 实现须遵守：

- 校验项目、需求、任务、计划项和目标类型/归属一致，仅允许本任务已经关联的计划；
  批内不能重复同一计划项，不得突破修改边界。
- 实际 CREATE 的 beforeSnapshot 为空；UPDATE 的 beforeSnapshot 完整且身份、父级、类型匹配，
  version 与当前数据一致，冲突拒绝。afterSnapshot 必须非空且类型匹配；
  不通过 delFlg 隐含删除，也不移动父级。
- CREATE 计划首次发布通过 CREATE 履历绑定唯一对象；后续任务修改该对象时记录 UPDATE，
  必须提供修改前快照，不能再次创建。UPDATE 计划始终指向已有对象。
- 发布子对象前父对象必须已创建并解析出真实主键。parentItemKey 解析只在同一需求正确层级内进行，
  已提供祖先 ID 与解析结果冲突时拒绝；不能静默覆盖。
- 候选不能决定最终生成 ID、版本或删除标识。历史的完整路径和前后快照来自实际保存数据。
- 当前态与本批履历在同一数据库事务中保存；数据库回滚不意味着代码或文件副作用回滚，
  保存不确定时不能自动重新执行任务。
- 单项幂等范围是 (projectId, requirementTaskId, executionId, requirementChangePlanItemId)。
  相同请求重放返回原记录，不重复更新；同键不同请求拒绝。
  幂等识别须先于可能因首次发布而改变的版本和 CREATE 状态校验。
- 已失败任务中得到核实的实际局部改动也可以记录；不依据任务成功状态自动推断改动，
  不自动变更任务状态、重试或进行验收。

ProjectChangeRecord 是可变 Bean；“历史只追加”由未来持久化实现约束，
抽象 Domain 不暴露历史修改或删除入口。

## 三个抽象 Domain

所有方法都是 abstract，均 implements UserInterface；无 DAO 注入、Spring 注解或占位实现。
参数与完整 Javadoc 以对应 Java 文件为准：

| 抽象类 | 方法 |
| --- | --- |
| ProjectStructureDomain | listApplications、getApplication、listModules、getModule、listFunctions、getFunction |
| RequirementChangeDomain | createPlans、getPlans、setTaskPlans、getTaskPlans |
| ProjectChangeDomain | publishTaskChanges、getChangesByApplication、getChangesByModule、getChangesByFunction、getChangesByRequirement、getChangesByTask |

结构查询按 projectId 限定，仅返回当前未删除数据。单项缺失、已删除或归属不匹配返回 null，
列表无结果返回空列表；返回对象与内部可变对象分离，列表按 id 升序。
计划查询、履历查询同样受项目范围限制，无结果返回空列表，按相应记录 id 升序。

getChangesByApplication 和 getChangesByModule 只返回该对象自身的履历，
必须同时匹配 targetType，不自动包含下级对象修改。
需求查询包含其全部任务和目标层级的履历，任务查询包含其各次 executionId 记录。
本轮没有定义新的公共异常类或 HTTP 错误映射。

## 验证与后续范围

ProjectElementSnapshotTest 验证可变实体/输入列表与历史快照隔离、输出不可改、
null/空列表及 null 元素失败边界；不模拟尚未实现的业务规则。
test-compile 和普通 verify 验证现有调用链兼容性与本地 JAR 回归。
不运行真实模型或新结构数据库验收，也不表示数据库持久化、页面浏览或 Agent 项目记忆已经可用。

后续由用户审阅字段与 Domain 后再确定实现；DataBeanDao 保持原接口，
按外键查询的技术 DAO/Mapper、DDL、接入现有规划执行及 UI 均留待具体实现阶段。
