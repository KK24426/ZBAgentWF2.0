# 注解注入最小示例

以下仅说明写法，不是已建立的业务接口。测试目录有跨包扫描样例，正式JAR不包含样例。

user 中定义接口：
```java
public interface Greeting {
    String greet();
}
```

agent 中提供实现：
```java
@Service
public class GreetingImpl implements Greeting {
    public String greet() { return "你好"; }
}
```

user 中的调用者也交给Spring：
```java
@Component
public class MyWorkflow {
    private final Greeting greeting;
    public MyWorkflow(Greeting greeting) { this.greeting = greeting; }
    public void execute() { greeting.greet(); }
}
```

在后续获批准的Web控制器中通过构造器注入MyWorkflow，再由用户定义的业务操作调用execute。
当前首页没有业务控制器或API；此示例不授权提前创建路由。
不要仅靠构造函数自动执行业务。接口存在多个实现时标记@Primary或在注入处使用@Qualifier。
new MyWorkflow(...)不触发Spring自动注入；自己new时应显式传依赖。
事务@Transactional应放在用户确定的服务边界上，并通过Spring代理从其它Bean调用；同类自调用不产生事务代理。
Mapper 接口放 `com.kk24426.zbagentwf.agent.persistence.mapper` 包并标注 `@Mapper`；XML 放 `src/main/resources/mapper`。当前只在 `mysql` profile 下注册 Mapper，依赖它的消费者也须按数据库启用条件组织。
完整的注解、参数和 XML 对应写法见 [Mapper 示例](../../src/main/resources/mapper/README.md)。使用 `#{value}` 绑定值；动态标识符必须白名单，不能直接拼接外部输入。
