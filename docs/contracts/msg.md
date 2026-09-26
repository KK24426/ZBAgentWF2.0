# 页面与接口消息契约

用户于2026-09-27批准中、英、日三语及外部覆盖。本契约只覆盖项目自有页面与Filter/Controller固定HTTP提示；日志、内部异常、启动诊断、容器原生错误、用户输入和模型生成内容不做本地化。

## 文件和查询

内置消息为`src/main/resources/msg/msg_zh_CN.properties`、`msg_en.properties`、`msg_ja.properties`，UTF-8，无需Unicode转义。稳定key按`page.*`、`language.*`、`chat.*`、`http.*`分类。技术工具`common.msg.MsgCatalog.get(String key, Locale locale, String... args)`返回纯文本；缺key或参数明确抛异常，不将key当作成功结果。未知语言查询回落简体中文。

参数只支持从零连续编号的`{0}`、`{1}`等，允许重复和调整顺序；不支持MessageFormat数字/日期/复数规则、HTML或嵌套展开。普通单引号原样保留。译文不得空白，不能把花括号用作普通文字。

根包MsgConfiguration启动时显式读取文件并生成不可变快照。启动目录下`config/msg/`同名文件只覆盖指定语言的已知key，缺文件或缺键保留内置值；不进行跨语言覆盖。三份内置key必须相同，动态分支必需key和首页占位符均检查存在。覆盖值必须保留原参数编号集合；未知key、重复key、空白消息、格式/编码错误或存在但不可读的文件使启动失败。错误不回显配置值，现有安全日志保留异常链。修改后重启，无热加载。

## 语言选择

`config/msg.properties`的`zb.msg.locale`只接受`auto`、`zh-CN`、`en`、`ja`，缺省`auto`；显式空白/非法值导致启动失败。配置源优先级为JVM `-Dzb.msg.locale` > 环境变量`ZB_MSG_LOCALE` > 文件。

一次请求按以下顺序决定语言：合法`zb.locale` Cookie > 显式服务语言 > `Accept-Language` > 简体中文。Cookie只接受三个规范语言标识，非法值忽略。浏览器语言按q权重降序，同权重按原顺序；中文/英文/日文地区变体按主语言匹配，繁体中文也使用当前唯一中文译文（简体中文）。q=0、通配符和未支持语言不参与明确匹配；无匹配最终回落中文。请求头畸形时整体忽略，不记录原值，也不把原404/405/415/503改变为500。

MsgLocaleResolver把选择缓存在当前request属性中。WebRequestFilter在拒绝路径前解析；MVC使用同一个LocaleResolver，不修改JVM Locale或共享当前语言。首页和固定HTTP错误返回Content-Language；模型reply原样返回，不推断其语言。

页面选择器提供自动、简体中文、English、日本語。手动选择即时更新文字、元信息、无障碍属性和已有固定状态；保留输入、模型回复、执行中的请求及请求编号。Cookie为`zb.locale`，保存一年、Path=/、SameSite=Lax，HTTPS加Secure；自动清除Cookie，重新使用服务/浏览器自动结果。浏览器禁用Cookie时即时切换仍可用，刷新持久化不保证。此Cookie不表示身份或权限，不创建服务端会话。

## 首页与安全

原`/`、`/index.html`继续接受GET/HEAD，HomePageController渲染非公开`web/index.html`模板；其它路由和状态码不变。仅固定key占位符替换，不引入模板表达式引擎。模板值统一HTML转义；前端三语消息先JSON序列化再做HTML文本转义，存入不可执行的`template#msg-catalog`，通过`content.textContent`读取。HTTP错误key不导出到前端；前端有独立的按状态码归类的安全提示。

翻译、输入及模型输出均不作为HTML执行；沿用CSP、自有脚本、无缓存与请求隐私保护。没有翻译下载API、设置写API、数据库或外部翻译调用。模板、内置消息和外部配置路径不可通过HTTP访问。无JavaScript时首屏与启用脚本提示仍按请求语言渲染。
