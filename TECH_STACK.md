# ParkingSystem 技术栈与方法总结

## 一、项目概述

智能停车管理系统，采用**客户端 + 管理端双端架构**，覆盖用户注册登录、附近停车场搜索、车位状态实时查询、停车订单生命周期、优惠券秒杀、钱包充值扣费等功能。管理端包含数据看板、车位/优惠券/用户管理。

---

## 二、核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.13 | 项目基础框架 |
| Java | 21 | 开发语言 |
| MyBatis-Plus | 3.5.16 | ORM，LambdaQueryWrapper 动态查询，分页插件 |
| Spring MVC | — | RESTful API 层 |
| Spring Validation | — | 参数校验（`@Validated` / `@NotBlank` / `@Pattern`） |
| Lombok | — | `@Data` / `@Slf4j` / `@RequiredArgsConstructor` 简化代码 |

---

## 三、数据库

| 技术 | 用途 |
|------|------|
| MySQL 8.x | 关系型数据库，11 张业务表 |
| MyBatis-Plus `BaseMapper<T>` | 通用 CRUD，无需手写 XML |
| `LambdaQueryWrapper` | 类型安全的动态条件查询 |
| `PaginationInnerInterceptor` | MyBatis-Plus 分页插件 |
| `MetaObjectHandler` | 自动填充 `createTime` / `updateTime` |
| 事务管理 | `@Transactional` + `TransactionSynchronizationManager.afterCommit()` |

---

## 四、缓存（三级架构）

```
请求 → Caffeine (L1) → Redis (L2) → MySQL (L3)
                    → 任一缓存失效自动降级下一层
```

### 4.1 Caffeine 本地缓存（7 个独立 Cache bean）

| Cache 名称 | 容量 | TTL | 用途 |
|-----------|------|-----|------|
| `couponAvailableCache` | 500 | 3 min | 可领取优惠券列表 |
| `parkingLotCache` | 1000 | 10 s | 停车场详情 |
| `vehicleCache` | 10000 | 2 min | 用户车辆列表 |
| `walletBalanceCache` | 10000 | 30 s | 钱包余额 |
| `couponDetailCache` | 500 | 5 min | 优惠券详情 |
| `couponStaticCache` | 500 | 5 min | 优惠券静态信息 |
| `parkingSpotCache` | 500 | 5 min | 车位列表 |

### 4.2 Redis 数据类型与用途

| 数据类型 | 用途 |
|---------|------|
| **String** | 会话缓存、短信验证码、登录失败计数器、账号锁定标记、JSON 对象缓存、秒杀库存计数器、月编辑次数、仪表盘计数 |
| **Hash** | 实时趋势数据（按月分 key）、管理端车位状态、可用车位数 |
| **Bitmap** | 车位占用状态（客户端，位运算实现 O(1) 查询） |
| **GEO** | 附近停车场搜索（`GEORADIUS` 按距离排序、限制 10 条） |
| **Set** | 秒杀抢购用户去重，防止重复领取 |

### 4.3 Redis 高级特性

| 特性 | 实现 | 用途 |
|------|------|------|
| 分布式锁 | Redisson `RLock` + Lua 脚本 | 车位状态重建防并发 |
| Lua 原子脚本 | `seckillCoupon.lua` / `ReleaseLock.lua` / `rebuildLot.lua` | 秒杀扣库存+去重原子操作、分布式锁释放 |
| 连接池 | Lettuce Pool（max-active=8） | Redis 连接复用 |
| 缓存穿透防护 | 空值缓存（停车场信息不存在时缓存空串） | 避免无效 key 穿透到 DB |
| 优雅降级 | `try-catch` 返回 null，非关键路径自动降级 | Redis 故障时不影响核心业务流程 |
| 临时表重建 | tempKey → `rename` 模式 | 避免缓存重建期间读到不完整数据 |
| 缓存失效广播 | RabbitMQ Fanout Exchange | 多实例部署时同步清除本地 Caffeine 缓存 |

---

## 五、消息中间件

| 技术 | 用途 |
|------|------|
| RabbitMQ | 异步消息，解耦核心业务与下游处理 |
| Direct Exchange | 秒杀订单精确路由 |
| Fanout Exchange | 多实例缓存失效广播 |
| AnonymousQueue | 每个服务实例独享队列（`exclusive: true`），自动清理 |
| `Jackson2JsonMessageConverter` | 消息 JSON 序列化/反序列化 |
| `@RabbitListener` | 声明式消息消费 |
| `RabbitTemplate` | 消息发送 |
| `SimpleRabbitListenerContainerFactory` | 自定义监听器容器 |

### 消息流

**秒杀订单：**
```
Controller → Lua 原子扣减 → SeckillMessageProducer 
    → Direct Exchange (seckill.order.exchange) 
    → Queue (seckill.order.queue) 
    → SeckillMessageConsumer → 异步写入 UserCoupon
```

**缓存失效广播：**
```
CouponService → CacheInvalidateProducer 
    → Fanout Exchange (cache.invalidate.exchange) 
    → 所有实例的 AnonymousQueue 
    → CacheInvalidateConsumer → 清除本地 Caffeine 缓存
```

---

## 六、安全认证

| 技术 | 用途 |
|------|------|
| JWT（jjwt 0.12.3） | Token 签发、解析、校验 |
| MD5 | 密码加密存储 |
| 拦截器链 | 三层拦截器依次执行 |
| ThreadLocal | `UserHolder` / `AdminHolder` 保存当前请求的用户/管理员 ID |
| Redis 会话管理 | 登录成功后将用户信息存入 Redis，每次请求刷新 TTL |
| 登录限流 | Redis 计数器记录失败次数，5 次失败锁定账号 5 分钟 |

### 拦截器链

| 顺序 | 拦截器 | 拦截范围 | 职责 |
|------|--------|---------|------|
| 1 | `RefreshTokenInterceptor` | `/**` 所有路径 | 解析 JWT，刷新 Redis 会话 TTL |
| 2 | `JwtTokenUserInterceptor` | 客户端 API（排除登录/注册） | 校验 JWT 有效性，检查 Redis 会话，存入 UserHolder |
| 2 | `JwtTokenAdminInterceptor` | 管理端 API（排除登录） | 校验 JWT 有效性，检查 Redis 会话，存入 AdminHolder |

---

## 七、设计模式与方法

### 7.1 架构模式

| 模式 | 实现 |
|------|------|
| **三层架构** | Controller → Service → Mapper |
| **门面模式** | `ParkingDataProvider` / `CouponDataProvider` 封装多级缓存读写逻辑 |
| **双端分离** | `client/` 客户端模块 + `admin/` 管理端模块，共享 `common/` 公共层 |

### 7.2 缓存策略

| 策略 | 实现 |
|------|------|
| **多级缓存** | Caffeine (L1) → Redis (L2) → DB (L3)，逐级降级 |
| **缓存穿透防护** | 空值缓存，DB 查询不到结果也缓存空串 |
| **缓存失效广播** | RabbitMQ Fanout 通知所有实例清除本地缓存 |
| **分布式锁重建** | Redisson `RLock` + tempKey → rename 防止重复重建 |
| **本地缓存预热** | `@PostConstruct` 在启动时加载优惠券库存到 Redis |
| **游标分页** | `WHERE (time, id) < (?, ?)` 二分法游标，避免深度分页 |

### 7.3 并发控制

| 场景 | 方案 |
|------|------|
| 秒杀扣库存 | Lua 脚本原子扣减（stock + 去重 Set 在同一条 Lua 中完成） |
| 普通优惠券领取 | 数据库 `UPDATE ... WHERE remain_stock > 0` 乐观锁 |
| 缓存重建 | Redisson `RLock` 互斥锁，tempKey → rename 原子切换 |
| 分布式锁释放 | Lua 脚本对比线程 ID 后删除，防止误删 |

### 7.4 容错与降级

| 场景 | 策略 |
|------|------|
| Redis 故障（关键路径：登录/注册） | 硬失败，抛 `BusinessException(500)` |
| Redis 故障（非关键路径：缓存查询） | 返回 null，自动降级到 DB |
| Redis 故障（秒杀） | 不允许直接成功，优先保证系统安全 |
| 管理端 Redis 故障 | 缓存仅作为加速，降级直接查 DB |
| `@RabbitListener` 异常 | 日志记录，不阻塞主流程 |
| 全局异常兜底 | `@RestControllerAdvice` 捕获所有未处理异常，返回统一错误响应 |

### 7.5 事务与缓存一致性

| 手段 | 实现 |
|------|------|
| 事务后清理缓存 | `TransactionSynchronizationManager.registerSynchronization` + `afterCommit()` |
| Spring Cache | `@Cacheable` / `@CacheEvict` 声明式缓存管理 |
| 显式 Redis 删除 | 事务提交后手动 `stringRedisTemplate.delete(key)` |

### 7.6 其他设计

| 模式/方法 | 实现 |
|-----------|------|
| 全局异常处理 | `@RestControllerAdvice` 统一处理：`BusinessException` → 指定 code、校验异常 → 400、系统异常 → 500 |
| 统一返回格式 | `Result<T>` 包含 code / message / data，`PageResult<T>` 包含分页信息 |
| 配置分离 | `@ConfigurationProperties(prefix="jwt")` 注入 JWT 配置 |
| 魔法值管理 | 所有硬编码字符串/数字提取到 `*Constant.java` |
| 自动填充 | `MetaObjectHandler` 统一处理实体创建/更新时间 |
| GEO 附近搜索 | Redis `GEORADIUS` 按距离排序，失败降级为 Haversine 公式计算 |

---

## 八、项目分层

```
com.xigeandwillian.parkingsystem/
├── ParkingSystemApplication.java          # 启动类
├── common/                                # 公共层
│   ├── cache/                             # 缓存封装（CacheResult, ParkingCache）
│   ├── config/                            # 配置类（Redis, Redisson, RabbitMQ, Caffeine, MyBatis-Plus, Jackson, WebMvc）
│   ├── constant/                          # 常量类（Redis/Order/Coupon/Cache/JWT/Result 常量）
│   ├── entity/                            # 11 张表实体
│   ├── exception/                         # BusinessException 自定义异常
│   ├── handler/                           # GlobalExceptionHandler + MetaObjectHandler
│   ├── interceptor/                       # 3 个拦截器（Token 刷新 + 用户/管理端认证）
│   ├── mapper/                            # 8 个 MyBatis-Plus Mapper
│   ├── properties/                        # JwtProperties 配置绑定
│   ├── result/                            # Result / PageResult 统一响应
│   ├── service/                           # RedisService + RedisServiceImpl
│   └── utils/                             # 工具类（JWT/Holder/缓存/距离/正则）
├── client/                                # 客户端 API
│   ├── controller/                        # 5 个 Controller（用户/停车场/车辆/优惠券/钱包）
│   ├── dto/                               # 请求体（登录/注册/编辑/车辆）
│   ├── mq/                                # RabbitMQ 消息（秒杀 + 缓存失效）
│   ├── service/                           # 业务实现
│   └── vo/                                # 响应体（停车场/优惠券/用户 VO）
└── admin/                                 # 管理端 API
    ├── controller/                        # 7 个 Controller（登录/看板/停车场/车位/优惠券/用户/管理员）
    ├── dto/                               # 请求体（登录/创建/编辑）
    ├── mapper/                            # 管理端专用 Mapper
    ├── service/                           # 业务实现
    └── vo/                                # 响应体（看板/停车场/车位/优惠券/用户 VO）
```

---

## 九、代码规范

| 规范 | 要求 |
|------|------|
| 异常处理 | 业务失败抛 `BusinessException(code, msg)`，不直接 `return Result.fail` |
| 日志级别 | 系统异常 `log.error`，业务异常 `log.warn`，流程日志 `log.info` |
| 入口日志 | 每个 Controller 方法必须写 `log.info("操作描述: {}", 关键参数)` |
| 参数校验 | 用 `@Pattern` / `@NotBlank`，Controller 加 `@Validated`，Service 不做校验 |
| 常量管理 | 魔法值提取到 `*Constant.java`，Redis Key 格式 `{模块}:{功能}:` |
| 类型一致 | VO/DTO 字段类型必须与 Entity 一致（`Long` 不写成 `Integer`） |
| Redis 使用 | 优先用 `RedisService`，不要直接用 `StringRedisTemplate`（验证码模块例外） |
| 命名规范 | 类/接口大驼峰，方法/参数小驼峰，常量全大写+下划线，包名全小写 |
