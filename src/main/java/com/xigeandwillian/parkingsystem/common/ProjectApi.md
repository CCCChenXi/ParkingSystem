# 智能停车系统 — 接口文档

---

## 通用约定

### 基础 URL
```
/api
```

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / Token 过期 |
| 403 | 权限不足 |
| 500 | 服务端错误 |

### 认证方式
```
Header: Authorization: Bearer <token>
```

### 拦截器白名单（无需 JWT）
```
POST   /api/user/login
POST   /api/user/register
POST   /api/user/send-code
POST   /api/admin/login
GET    /api/parking-lots/nearby
GET    /api/parking-lots/{id}
GET    /api/parking-lots/{id}/spots
```

---

## 一、公共接口（无需 JWT）

### 1.1 用户注册
```
POST /api/user/register
```

**Request**
```json
{ "username": "张三", "password": "123456", "phone": "13800138000", "code": "123456" }
```

**Response `data`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": { "id": 1, "username": "张三", "phone": "13800138000", "avatar": "" }
}
```

---

### 1.2 用户登录
```
POST /api/user/login
```

**Request**
```json
{ "username": "张三", "password": "123456" }
```

**Response `data`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": { "id": 1, "username": "张三", "phone": "13800138000", "avatar": "" }
}
```

---

### 1.3 附近停车场 (Redis GEO)
```
GET /api/parking-lots/nearby?longitude=113.95&latitude=22.54&radius=5000
```

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| longitude | number | — | 当前经度 |
| latitude | number | — | 当前纬度 |
| radius | number | 5000 | 搜索半径（米），可选 |

**Response `data`**
```json
[
  {
    "id": 1,
    "name": "科技园停车场",
    "address": "南山区科技南路100号",
    "longitude": 113.95,
    "latitude": 22.54,
    "totalSpots": 100,
    "availableSpots": 45,
    "imageUrl": "",
    "status": 1,
    "distance": "0.3km"
  }
]
```

> `distance` 由 Redis `GEORADIUS` 的 `WITHDIST` 返回

---

### 1.4 停车场详情
```
GET /api/parking-lots/{id}
```

**Response `data`**
```json
{
  "id": 1,
  "name": "科技园停车场",
  "address": "南山区科技南路100号",
  "longitude": 113.95,
  "latitude": 22.54,
  "totalSpots": 100,
  "availableSpots": 45,
  "imageUrl": "",
  "status": 1
}
```

---

### 1.5 车位列表（含 Redis Bitmap 状态）
```
GET /api/parking-lots/{id}/spots
```

**Response `data`**
```json
[
  { "id": 1, "spotNumber": "A01", "type": 0, "status": 1 },
  { "id": 2, "spotNumber": "A02", "type": 2, "status": 0 }
]
```

| 字段 | 说明 |
|------|------|
| `id` | 车位 ID（作为 Bitmap offset） |
| `spotNumber` | 编号 A01 |
| `type` | 0 = 标准 1 = 大型 2 = 充电桩 |
| `status` | 0 = 空闲 1 = 占用（来自 Redis `GETBIT parking:spots:{lotId} {spotId}`） |

> 后端实现：查 DB `parking_spot` 元数据 + Redis Bitmap 组装返回

---

### 1.6 管理员登录
```
POST /api/admin/login
```

**Request**
```json
{ "username": "admin", "password": "123456" }
```

**Response `data`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "admin": { "id": 1, "username": "admin", "role": "super" }
}
```

### 1.7 发送验证码
```
POST /api/user/send-code
```

**Request**
```json
{ "phone": "13800138000" }
```

**后端逻辑**
1. 生成 6 位随机验证码
2. 存入 Redis：`SETEX sms:code:{phone} 300 {code}`（5 分钟有效）
3. 调用短信网关发送（对接第三方 SMS 服务）

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "验证码已发送" }
```

---

## 二、客户端接口（需 JWT）

### 2.1 用户信息

#### 获取用户信息
```
GET /api/user/profile
```

**Response `data`**
```json
{ "id": 1, "username": "张三", "phone": "13800138000", "avatar": "" }
```

#### 修改用户信息
```
PUT /api/user/profile
```

**Request**
```json
{ "phone": "13900000000", "avatar": "url" }
```

**Response `data`**
```json
{ "id": 1, "username": "张三", "phone": "13900000000", "avatar": "https://..." }
```

---

### 2.2 车辆管理

#### 车辆列表
```
GET /api/vehicles
```

**Response `data`**
```json
[
  { "id": 1, "plateNumber": "粤B·88888", "brand": "特斯拉 Model 3", "color": "白色" }
]
```

#### 添加车辆
```
POST /api/vehicles
```

**Request**
```json
{ "plateNumber": "粤B·88888", "brand": "特斯拉 Model 3", "color": "白色" }
```

**Response `data`**
```json
{ "id": 2, "plateNumber": "粤B·88888", "brand": "特斯拉 Model 3", "color": "白色" }
```

#### 修改车辆
```
PUT /api/vehicles/{id}
```

**Request**
```json
{ "plateNumber": "粤B·88888", "brand": "特斯拉", "color": "黑色" }
```

**Response `data`**
```json
{ "id": 1, "plateNumber": "粤B·88888", "brand": "特斯拉", "color": "黑色" }
```

#### 删除车辆
```
DELETE /api/vehicles/{id}
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "删除成功" }
```

---

### 2.3 订单管理

#### 预约车位（分布式锁）
```
POST /api/orders
```

**Request**
```json
{ "lotId": 1, "spotId": 100, "plateNumber": "粤B·88888", "couponId": 4 }
```

**后端逻辑**

1. `SET lock:reserve:{lotId}:{spotId} {userId} NX EX 900` — 获取分布式锁（15min TTL）
2. `GETBIT parking:spots:{lotId} {spotId}` — 检查是否空闲
3. `SETBIT parking:spots:{lotId} {spotId} 1` — 标记占用
4. 创建订单（status=0）、扣减 availableSpots
5. 发送 RabbitMQ `order.reserved` → 生成消息通知
6. Lua 脚本释放锁

**Response `data`**
```json
{
  "id": 1,
  "orderNo": "ORD20260623001",
  "status": 0,
  "createTime": "2026-06-23 14:30:00"
}
```

---

#### 订单列表（按状态筛选）
```
GET /api/orders?status=0
```

| status | 含义 |
|--------|------|
| 0 | 已预约 |
| 1 | 进行中 |
| 2 | 已结算 |
| 3 | 已取消 |

**Response `data`**
```json
[
  {
    "id": 1,
    "orderNo": "ORD20260623001",
    "lotId": 1,
    "lotName": "科技园停车场",
    "spotNumber": "A01",
    "plateNumber": "粤B·88888",
    "status": 0,
    "startTime": "",
    "endTime": "",
    "amount": 0,
    "discount": 0,
    "createTime": "2026-06-23 14:30"
  }
]
```

---

#### 订单详情
```
GET /api/orders/{id}
```

**Response `data`**
```json
{
  "id": 1,
  "orderNo": "ORD20260623001",
  "lotId": 1,
  "lotName": "科技园停车场",
  "spotNumber": "A01",
  "plateNumber": "粤B·88888",
  "status": 0,
  "startTime": "",
  "endTime": "",
  "amount": 0,
  "discount": 0,
  "createTime": "2026-06-23 14:30"
}
```

#### 确认入场
```
PUT /api/orders/{id}/enter
```

**后端逻辑**
1. 订单 status 0 → 1
2. 记录 `startTime = now()`
3. 发送 RabbitMQ `order.enter`

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "入场成功" }
```

---

#### 结算订单
```
PUT /api/orders/{id}/settle
```

**计费公式**
```
duration   = ceil((exitMillis - enterMillis) / 3600000)
amount     = duration × hourlyRate
payable    = amount - discount
```

**后端逻辑**
1. 订单 status 1 → 2
2. 计算费用、记录 `endTime = now()`
3. `SETBIT parking:spots:{lotId} {spotId} 0` — 释放车位
4. 扣减钱包余额
5. 发送 RabbitMQ `order.settle`

**Response `data`**
```json
{
  "id": 1,
  "amount": 15.00,
  "discount": 5,
  "payable": 10.00,
  "balance": 118.50
}
```

---

#### 取消预约
```
PUT /api/orders/{id}/cancel
```

**后端逻辑**
1. 订单 status 0 → 3
2. `SETBIT parking:spots:{lotId} {spotId} 0` — 释放车位
3. 增加 availableSpots
4. 删除分布式锁

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "已取消" }
```

---

### 2.4 消息管理

#### 消息列表
```
GET /api/messages
```

**Response `data`**
```json
[
  {
    "id": 1,
    "title": "预约成功",
    "content": "科技园停车场A01已为您保留",
    "type": 0,
    "isRead": 0,
    "createTime": "2026-06-23 14:30"
  }
]
```

| type | 含义 |
|------|------|
| 0 | 预约通知 |
| 1 | 入场提醒 |
| 2 | 结算通知 |
| 3 | 系统消息 |

#### 标记已读
```
PUT /api/messages/{id}/read
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "success" }
```

#### 全部已读
```
PUT /api/messages/read-all
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "success" }
```

---

### 2.5 优惠券

#### 可领取优惠券列表
```
GET /api/coupons/available
```

**Response `data`**
```json
[
  {
    "id": 1,
    "name": "新用户专享",
    "description": "满10减5",
    "discountAmount": 5,
    "minAmount": 10,
    "type": 0,
    "stock": 100,
    "remainStock": 88,
    "startTime": "2026-06-01",
    "endTime": "2026-12-31"
  }
]
```

#### 领取普通优惠券
```
POST /api/coupons/claim/{id}
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "领取成功" }
```

#### 秒杀优惠券
```
POST /api/coupons/flash/{id}
```

**后端逻辑（Lua 脚本保证原子性）**
```lua
local stock = redis.call('DECR', 'coupon:flash:' .. KEYS[1])
if stock < 0 then
  redis.call('INCR', 'coupon:flash:' .. KEYS[1])
  return -1
end
return stock
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "秒杀成功" }
```

#### 我的优惠券
```
GET /api/coupons?scope=mine
```

**Response `data`**
```json
[
  {
    "id": 4,
    "name": "满20减5",
    "discountAmount": 5,
    "minAmount": 20,
    "type": 0,
    "status": 0,
    "startTime": "2026-06-01",
    "endTime": "2026-07-01"
  }
]
```

| status | 含义 |
|--------|------|
| 0 | 未使用 |
| 1 | 已使用 |
| 2 | 已过期 |

---

### 2.6 钱包

#### 钱包信息
```
GET /api/wallet
```

**Response `data`**
```json
{ "id": 1, "balance": 128.50 }
```

#### 充值
```
POST /api/wallet/recharge
```

**Request**
```json
{ "amount": 100 }
```

**Response `data`**
```json
{ "balance": 228.50 }
```

#### 钱包流水
```
GET /api/wallet/logs
```

**Response `data`**
```json
[
  { "id": 1, "amount": 100, "type": 0, "remark": "微信充值", "createTime": "2026-06-20 14:30" },
  { "id": 2, "amount": -25, "type": 1, "remark": "停车费-科技园停车场", "createTime": "2026-06-22 11:30" }
]
```

| type | 含义 |
|------|------|
| 0 | 充值 |
| 1 | 支付 |

---

### 2.7 退出登录

#### 用户退出
```
POST /api/user/logout
```

**后端逻辑**
1. 根据 JWT 中的 `userId` 清除 Redis session：`DEL session:{userId}`
2. 前端清除 localStorage 中的 `token` 和 `user`

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "退出成功" }
```

---

## 三、管理端接口（需 Admin JWT）

### 3.1 仪表盘
```
GET /api/admin/dashboard
```

**Response `data`**
```json
{
  "lotCount": 12,
  "spotCount": 350,
  "todayOrders": 128,
  "todayRevenue": 5200.00,
  "orderTrend": [
    { "date": "06/17", "orders": 85 },
    { "date": "06/18", "orders": 92 }
  ],
  "revenueTrend": [
    { "date": "06/17", "revenue": 3200 },
    { "date": "06/18", "revenue": 3800 }
  ],
  "recentOrders": [
    { "lotName": "科技园停车场", "plate": "粤B·88888", "status": "进行中", "time": "10:15" }
  ]
}
```

---

### 3.2 停车场管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/parking-lots` | 列表 |
| POST | `/api/admin/parking-lots` | 新增 |
| PUT | `/api/admin/parking-lots/{id}` | 编辑 |
| DELETE | `/api/admin/parking-lots/{id}` | 删除 |

**GET Response `data`**
```json
[
  {
    "id": 1,
    "name": "科技园停车场",
    "address": "南山区科技南路100号",
    "totalSpots": 100,
    "availableSpots": 45,
    "longitude": 113.95,
    "latitude": 22.54,
    "status": 1
  }
]
```

**新增/编辑 Request**
```json
{ "name": "新停车场", "address": "地址", "totalSpots": 100, "longitude": 113.95, "latitude": 22.54, "status": 1 }
```

> 新增时需要同步写入 Redis GEO：`GEOADD parking:lot:geo {lng} {lat} {lotId}`

---

### 3.3 车位管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/parking-spot/{lotId}` | 某停车场车位列表 |
| POST | `/api/admin/parking-spot` | 批量新增 |
| DELETE | `/api/admin/parking-spot/{lotId}/{id}` | 删除 |
| PUT | `/api/admin/parking-spot/{lotId}/{id}` | 编辑 |

**GET Response `data`**
```json
[
  { "id": 1, "spotNumber": "A01", "type": 0, "status": 1 },
  { "id": 2, "spotNumber": "A02", "type": 2, "status": 0 }
]
```

**批量新增 Request**
```json
{ "lotId": 1, "spotNumbers": ["E01", "E02", "E03"], "type": 0 }
```

> 新增后初始化 Redis Bitmap：`SETBIT parking:spots:{lotId} {spotId} 0`

---

### 3.4 优惠券管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/coupons` | 列表 |
| POST | `/api/admin/coupons` | 新增 |
| PUT | `/api/admin/coupons/{id}` | 编辑 |
| DELETE | `/api/admin/coupons/{id}` | 删除 |

**GET Response `data`**
```json
[
  {
    "id": 1,
    "name": "满20减5",
    "description": "停车优惠",
    "discountAmount": 5,
    "minAmount": 20,
    "type": 0,
    "stock": 100,
    "remainStock": 88,
    "startTime": "2026-06-01",
    "endTime": "2026-12-31"
  }
]
```

**新增/编辑 Request**
```json
{
  "name": "满20减5",
  "description": "停车优惠",
  "discountAmount": 5,
  "minAmount": 20,
  "type": 0,
  "stock": 100,
  "startTime": "2026-06-01",
  "endTime": "2026-12-31"
}
```

> 新增秒杀优惠券时初始化 Redis：`SET coupon:flash:{id} {stock}`

---

### 3.5 用户管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| GET | `/api/admin/users/{id}` | 用户详情 |

**GET 列表 Response `data`**
```json
[
  {
    "id": 1,
    "username": "张三",
    "phone": "13800138000",
    "vehicles": 2,
    "orderCount": 15,
    "balance": 128.50,
    "createTime": "2026-01-15 10:00"
  }
]
```

**GET 详情 Response `data`**
```json
{
  "id": 1,
  "username": "张三",
  "phone": "13800138000",
  "vehicles": 2,
  "orderCount": 15,
  "balance": 128.50,
  "createTime": "2026-01-15 10:00"
}
```

---

### 3.6 管理员管理

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/admins` | 列表 |
| POST | `/api/admin/admins` | 新增 |

**GET Response `data`**
```json
[
  { "id": 1, "username": "admin", "role": "super", "createTime": "2026-01-01 00:00" },
  { "id": 2, "username": "operator1", "role": "operator", "createTime": "2026-06-01 12:00" }
]
```

**新增管理员 Request**
```json
{ "username": "operator1", "password": "123456", "role": "operator" }
```

---

### 3.7 修改个人信息
```
PUT /api/admin/profile
```

**Request**
```json
{ "username": "admin", "oldPassword": "", "newPassword": "123456" }
```

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "修改成功" }
```

---

### 3.8 退出登录

#### 管理员退出
```
POST /api/admin/logout
```

**后端逻辑**
1. 根据 JWT 中的 `adminId` 清除 Redis admin session：`DEL admin:session:{adminId}`
2. 前端清除 localStorage 中的 `adminToken` 和 `adminUser`

**Response** `data` 为空，仅返回统一信封：
```json
{ "code": 200, "message": "退出成功" }
```

---

## 四、Redis Key 设计速查

| Key | 类型 | 用途 |
|-----|------|------|
| `parking:lot:geo` | GEO | 存储停车场经纬度 |
| `parking:spots:{lotId}` | Bitmap | 车位状态（offset = spot.id） |
| `lock:reserve:{lotId}:{spotId}` | String | 分布式锁，TTL = 900s |
| `cache:parking:lots` | String | 停车场列表缓存 |
| `coupon:flash:{couponId}` | String | 秒杀库存 |

---

## 五、RabbitMQ 设计

| 属性 | 值 |
|------|-----|
| Exchange | `parking.exchange`（topic） |

| 路由 key | 触发时机 | 消费者动作 |
|----------|----------|-----------|
| `order.reserved` | 预约成功 | 插入消息通知 |
| `order.enter` | 确认入场 | 插入入场消息 |
| `order.settle` | 结算完成 | 插入结算消息 |

---

## 六、定时任务

| 任务 | 间隔 | 逻辑 |
|------|------|------|
| 清理超时预约 | 每分钟 | `status = 0` 且 `createTime + 15min < now()` → 取消订单、释放 Bitmap、释放锁、推送消息 |
