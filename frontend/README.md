# AI Todo Frontend

这是 `ai-todo` 项目的 Vue 前端，采用前后端分离模式。

## 当前已对接的后端接口

- `POST /api/users/register`

请求体：

```json
{
  "username": "qijx",
  "email": "qijx@example.com",
  "password": "123456"
}
```

响应体：

```json
{
  "id": 1,
  "username": "qijx",
  "email": "qijx@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-07-06T21:00:00"
}
```

## 启动方式

先启动后端 Spring Boot 项目，再启动前端：

```bash
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

开发环境中，前端会把 `/api` 请求代理到：

```text
http://localhost:8080
```

如后端端口不同，可以复制 `.env.example` 为 `.env` 后修改：

```bash
VITE_API_PROXY_TARGET=http://localhost:8081
```
