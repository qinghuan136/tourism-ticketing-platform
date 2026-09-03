# 前端应用

`tourist` 和 `operator` 是两个独立的 Vue 应用，分别面向游客和运营管理人员；它们不直接导入彼此的源代码。

## 本地开发

在目标应用目录执行：

```bash
npm install
npm run dev
```

默认开发端口：游客端 `5173`，运营端 `5174`。两端都会把 `/api/*` 请求代理到 `http://localhost:8080/*`；可通过 `VITE_API_PROXY_TARGET` 覆盖目标地址。

## 构建

```bash
npm run type-check
npm run build
```

生产环境建议由 Web 服务器托管各应用的 `dist/`，并将 `/api` 反向代理到 Spring Boot 服务。
