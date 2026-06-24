# 用户认证模块 API 文档

## 通用说明

| 项目 | 值 |
|---|---|
| **Base URL** | `http://localhost:8080`（开发环境通过 Vite 代理 `/api` → `http://localhost:8080`） |
| **Content-Type** | `application/json` |
| **认证方式** | `Authorization: Bearer <token>` |
| **统一响应格式** | `{ "code": number, "msg": string, "data": T }` |

---

## 通用错误码

| HTTP 状态码 | code | msg | 前端行为 |
|---|---|---|---|
| 200 | ≠200 | 业务错误描述 | 根据具体 code 提示 |
| 401 | — | 未认证/token 过期 | 清除 token，跳转登录页 |
| 403 | — | 无权限 | 弹窗"无权限访问，请联系管理员" |
| 500 | — | 服务器内部错误 | 弹窗"服务器异常，请稍后重试" |

---

## 1. 发送短信验证码

注册时向用户手机发送验证码（阿里云 SMS）。

```
POST /api/user/send-code
```

### Request

```json
{
  "phone": "13800138000"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| phone | string | 是 | 11 位手机号，以 1 开头 |

### Response 200

```json
{
  "code": 200,
  "msg": "验证码已发送",
  "data": null
}
```

### Response 400

```json
{
  "code": 400,
  "msg": "手机号格式错误",
  "data": null
}
```

> 前端提示：`手机号格式错误`

---

## 2. 用户注册

```
POST /api/user/register
```

### Request

```json
{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "code": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码，≥ 6 位 |
| phone | string | 是 | 11 位手机号 |
| code | string | 是 | 短信验证码，6 位数字 |

### Response 200 — 注册成功

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "username": "testuser",
      "phone": "13800138000",
      "avatar": ""
    }
  }
}
```

> 前端收到 code=200 后：保存 token 和 user 到 localStorage → 跳转 `/home`

### Response 400 — 验证码错误

```json
{
  "code": 400,
  "msg": "验证码错误",
  "data": null
}
```

> 前端提示：`验证码错误，请重新输入`

### Response 500 — 服务器异常

```json
{
  "code": 500,
  "msg": "服务器内部错误",
  "data": null
}
```

> 前端提示：`服务器异常，请稍后重试`

---

## 3. 用户登录

```
POST /api/user/login
```

### Request

```json
{
  "username": "testuser",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

### Response 200 — 登录成功

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "username": "testuser",
      "phone": "13800138000",
      "avatar": ""
    }
  }
}
```

> 响应格式与注册一致。前端保存 token 到 localStorage，跳转 `/home`

---

## 4. 获取用户信息

```
GET /api/user/profile
Authorization: Bearer <token>
```

### Response 200

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "phone": "13800138000",
    "avatar": ""
  }
}
```

---

## 5. 更新用户信息

```
PUT /api/user/profile
Authorization: Bearer <token>
```

### Request

```json
{
  "username": "newname",
  "phone": "13800138001",
  "avatar": "http://example.com/avatar.png"
}
```

（所有字段可选，只传需要修改的字段）

### Response 200

```json
{
  "code": 200,
  "msg": "更新成功",
  "data": { ... }
}
```

---

## 6. 退出登录（纯前端）

清除 `localStorage` 中的 `token` 和 `user`，跳转 `/login`。

---

## 附：前端校验规则（供后端参考）

| 字段 | 规则 | 期望后端 code |
|---|---|---|
| phone | `/^1\d{10}$/` | 400 |
| code | 6 位数字 | 400 |
| password | ≥ 6 位 | — |
| username | 非空 | — |
