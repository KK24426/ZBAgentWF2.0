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

在已批准的CLI命令处理中注入或从容器取得MyWorkflow后调用execute。
不要仅靠构造函数自动执行业务。接口存在多个实现时标记@Primary或在注入处使用@Qualifier。
new MyWorkflow(...)不触发Spring自动注入；自己new时应显式传依赖。
事务@Transactional应放在用户确定的服务边界上，并通过Spring代理从其它Bean调用；同类自调用不产生事务代理。
Mapper XML放resources/mapper，使用#{value}绑定值；动态标识符必须白名单，不能直接拼接外部输入。
