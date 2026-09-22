# 用户骨架契约

本页记录用户已定义并批准补充的类型，不扩展根 AGENTS.md 的权限边界，不代表业务能力已经实现。

## user 包

- `UserImpl`：公共空父接口，用户编写的接口通过 `extends UserImpl` 继承。保留用户命名，当前没有业务方法，也不自动注册为 Spring Bean。
- `AgentBase`：用户定义的抽象类占位，本轮保持原样，基础能力方法待用户定义。
- `UserService`：用户定义的服务父类占位，本轮保持原样，没有业务行为或 Spring 注解。

UserImpl 原先为普通类，用户已明确批准将其改为父接口。类转接口通常存在源码和二进制兼容性差异；本次仓库检索没有调用方，项目不承诺 Maven 类库兼容。未来不应以 `new UserImpl()` 或类继承方式使用它。

## common.AgentBean

普通可变数据对象，保留公共无参构造，不注册为 Spring Bean，不执行模型调用或持久化。

| 私有 String 属性 | 用户定义含义 | 公开访问方法 |
| --- | --- | --- |
| brand | Agent 模型提供方 | getBrand / setBrand |
| name | 具体模型名称 | getName / setName |
| ver | 具体版本 | getVer / setVer |

三个属性相互独立，默认 null；getter 返回 String，setter 接受 String 并返回 void。允许 null、空字符串、中文和空白字符，原样存取，不增加校验、规范化或默认值。
不增加业务字段、构造参数要求、equals/hashCode/toString 或新的依赖；后续字段含义及业务约束仍由用户定义。

## 验证边界

AgentBeanTest 覆盖属性契约，UserImplTest 仅用测试内子接口和实现类验证 Java 类型关系；这些样例不进入正式 JAR，不构成新的业务契约。
现有 ContextTest 是 Spring 扫描与注入回归；目前没有真实业务调用方，因此不能把它视为业务链路验收。
