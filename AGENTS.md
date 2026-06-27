
# ParkingSystem 代码规范

## 命名

- 类/接口大驼峰，方法/参数小驼峰，常量全大写+下划线，包名全小写
- TTL 结尾必须带单位 `_TTL_MIN`/`_HOUR`/`_DAY`
- Redis Key 格式 `{模块}:{功能}:`，与常量名对应如：USER_LOGIN_COUNT = "user:login:count:";

## 异常

- 业务失败抛 `BusinessException(code, msg)`，code 取自 `ResultConstant`
- 不要 `return Result.fail`、和抛裸 `RuntimeException`
- 系统异常 `log.error`，业务异常 `log.warn`，流程日志 `log.info`

## 入口日志

- 每个 Controller 方法要写 `log.info("操作描述: {}", 关键参数)`

## Redis

- 优先用 `RedisService`（验证码模块例外），不要直接用 `StringRedisTemplate` 如果要直接用,需要处理抛出的异常
- 非关键路径降级（返回 null 判空），关键路径硬失败抛 `BusinessException(500)` 
- 在管理端中，redis仅仅作为缓存使用，在redis宕机时可以降级处理
- 在登录中，redis不只是作为缓存使用，而是记录了登录次数，封禁时间，如果降级处理，将会直接访问数据库，导致账号密码不安全
- 在注册功能中，redis用来存储验证码，如果redis宕机，我们不应该让用户直接成功创建账号，优先保证系统安全

## 校验

- 用 `@Pattern/@NotBlank`去校验参数非空和格式正确，Controller 需要加 `@Validated`
- Service 不做参数校验

## 常量

- 不要魔法值，所有硬编码字符串/数字提取到 `*Constant.java`

## 其他

- VO/DTO 字段类型必须与 Entity 一致（`Long` 不写成 `Integer`）
