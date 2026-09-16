# Nacos Config

默认 Group 为 `TOURISM`，DataId：`gateway.yml`、`user-service.yml`、`venue-service.yml`、`order-service.yml`、`coupon-service.yml`。

配置文件不包含数据库密码、JWT 密钥、内部服务令牌或第三方密钥。发布本目录配置可执行：

```powershell
.\nacos-config\publish-nacos-config.ps1
```

启用 Nacos 鉴权时，通过环境变量 `NACOS_ACCESS_TOKEN` 提供管理接口令牌。

Docker Compose 启动时会自动执行同目录的 `publish-nacos-config.sh`，将这些 DataId 发布到容器内 Nacos。
