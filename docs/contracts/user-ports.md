# 用户骨架契约

本页记录用户已定义并批准补充的类型，不扩展根 AGENTS.md 的权限边界，不代表业务能力已经实现。

## user 包

- `UserInterface`：由 UserImpl 更名的公共空父接口，没有业务方法，也不自动注册为 Spring Bean。
- `user.agent.userif.AgentBase`：用户定义的本机 Agent 发现、查询和刷新抽象契约，implements UserInterface；由 agent.registry.AgentCatalog 实现配置注册及可执行文件快照，精确查询使用brand/name/ver。聊天实现不继承此类型。
- `UserService`：用户定义的服务父类占位，本轮保持原样，没有业务行为或 Spring 注解。

用户已批准 UserImpl 更名、AgentBase 和 AgentBean 移包，以及[项目与执行契约](./project-agent.md)的字段和方法补齐。调用方需迁移类型名、import 和方法签名，不保留旧类型兼容层；项目不承诺 Maven 类库兼容。

## common.agent.bean.AgentBean

普通可变数据对象，保留公共无参构造，不注册为 Spring Bean，不执行模型调用或持久化。

| 私有 String 属性 | 用户定义含义 | 公开访问方法 |
| --- | --- | --- |
| brand | Agent 模型提供方 | getBrand / setBrand |
| name | 具体模型名称 | getName / setName |
| ver | 具体版本 | getVer / setVer |

三个属性相互独立，默认 null；getter 返回 String，setter 接受 String 并返回 void。允许 null、空字符串、中文和空白字符，原样存取，不增加校验、规范化或默认值。
不增加业务字段、构造参数要求、equals/hashCode/toString 或新的依赖；后续字段含义及业务约束仍由用户定义。

## 验证边界

AgentBeanTest 覆盖属性契约，UserInterfaceTest 用测试内子接口和实现类验证 Java 类型关系；这些样例不进入正式 JAR。
ContextTest 在显式提供项目根目录后验证 Spring 扫描与注入，不构成真实 Agent 业务链路验收。
