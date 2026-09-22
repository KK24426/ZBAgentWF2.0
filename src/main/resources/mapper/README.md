# Mapper XML

Agent 根据用户批准的表结构和业务接口在这里编写 XML。当前扫描配置只注册
`com.kk24426.zbagentwf.agent.persistence.mapper` 包及其子包中带 `org.apache.ibatis.annotations.Mapper` 注解的接口；仅放入包中而不加 `@Mapper` 不会被这条扫描规则注册。

## 最小对应示例

以下仅说明接口与 XML 写法，不是已有生产接口，不创建业务表，也不要求把示例加入源码。

接口文件示意：`src/main/java/com/kk24426/zbagentwf/agent/persistence/mapper/EchoMapper.java`。

```java
package com.kk24426.zbagentwf.agent.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EchoMapper {
    String echo(@Param("value") String value);
}
```

对应 XML 文件示意：`src/main/resources/mapper/EchoMapper.xml`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.kk24426.zbagentwf.agent.persistence.mapper.EchoMapper">
    <select id="echo" resultType="java.lang.String">
        SELECT #{value}
    </select>
</mapper>
```

- `namespace` 必须等于接口完整类名，语句 `id` 对应方法名，`#{value}` 对应 `@Param("value")`。
- 示例使用 XML 定义 SQL，不同时给同一方法添加 `@Select` 等重复映射。
- 只有启用 `mysql` profile 并提供有效数据源配置，才会加载当前 Mapper 扫描及 `classpath*:/mapper/**/*.xml`；启动时会验证真实数据库连接。
- 消费者交给 Spring 管理，通过构造器注入 Mapper；默认无数据库模式下不能无条件依赖这些 Mapper，数据库消费者也应限定在 `mysql` profile。
- 值使用 `#{parameter}` 绑定；动态表名、列名须来自明确白名单，不拼接外部原始输入，不写入自动建表脚本。

连接配置与专用库验收见[本地开发](../../../../docs/operations/local-dev.md)。
