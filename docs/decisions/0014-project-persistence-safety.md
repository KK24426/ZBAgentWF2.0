# ADR 0014：项目协调器、持久化与调用安全审核

状态：用户于2026-09-28明确批准接口补齐及实现。

Project保存三角色AgentBean，工厂按Project返回唯一协调器；execTasks(Long,callback)选择任务，execAllTasks(callback)提供批次最终回调。需求和任务分两阶段规划。目录从zb.project.root/local/UUID推导，local暂代未来用户ID。

Prompt不可变，附件防御复制并预留，构造无外部副作用；最终业务输入经同一配置模型独立只读审核，严格失败关闭。许可绑定完整调用且单次使用；模型预审不替代进程沙箱与目录约束。reviewAgent业务审核未扩展。

用户批准四表、DataBean元数据、Long数据库任务ID、完整聚合短事务及项目名称。模型调用与SQL事务分离，提交后发布ID/version；保存不确定隔离并重载，不自动重跑RUNNING。完整读取一致事务，内存仅维护canonical身份。未启用mysql时项目操作503，无存储降级。应用不自动迁移，旧内存数据不导入。

替代ADR0012中项目仅存内存和按Agent实体绑定执行器的部分；保留通用MemoryStore、三角色明确选择、规则文件、共享资源和安全HTTP边界。接口、迁移及限制以[项目契约](../contracts/project-agent.md)为准。
