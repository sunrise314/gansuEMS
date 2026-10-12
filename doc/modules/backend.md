# Backend 模块说明

Backend 运行在云端服务器，负责聚合分布式 Edge 系统，提供远程监控、数据存储和 API 接口。

---

## 核心模块

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.application` | Backend 应用入口，启动所有组件 |
| `io.openems.backend.core` | Backend 核心组件，管理组件生命周期 |
| `io.openems.backend.edge.manager` | Edge 管理器，维护所有连接的 Edge 设备列表和状态 |
| `io.openems.backend.common` | Backend 公共 API 和抽象基类 |

---

## 元数据 (Metadata)

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.metadata.odoo` | 基于 Odoo ERP 的元数据管理（默认生产用） |
| `io.openems.backend.metadata.file` | 基于文件的元数据管理（开发/测试用） |
| `io.openems.backend.metadata.dummy` | 空实现，用于测试 |

> 元数据存储：Edge 设备列表、用户、权限、电站配置等业务数据。

---

## 时序数据 (Timedata)

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.timedata.influx` | InfluxDB 时序数据存储（默认） |
| `io.openems.backend.timedata.aggregatedinflux` | 聚合 InfluxDB，多存储聚合 |
| `io.openems.backend.timedata.dummy` | 空实现，用于测试 |
| `io.openems.backend.timedata.iotdb` | **[新增]** Apache IoTDB 时序存储适配层 |

---

## API 接口

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.uiwebsocket` | 前端 UI 的 WebSocket 接口，实时推送数据 |
| `io.openems.backend.b2bwebsocket` | B2B WebSocket 接口，第三方系统集成 |
| `io.openems.backend.b2brest` | B2B REST API 接口 |

---

## 认证与安全

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.authentication.api` | 认证公共 API |
| `io.openems.backend.authentication.oauth2` | OAuth2 认证实现 |
| `io.openems.backend.oauthregistry` | OAuth 客户端注册管理 |

---

## 其他服务

| 模块名 | 功能说明 |
|---|---|
| `io.openems.backend.alerting` | 告警服务，监控阈值并发送告警通知 |
| `io.openems.backend.metrics.prometheus` | Prometheus 指标导出，用于监控 |
| `io.openems.backend.simulator` | Edge 模拟器，用于后端开发测试 |

---

## 架构关系图

```
UI 浏览器 ──┐
           ├─→ uiwebsocket ──→ edge.manager ──→ Edge 设备
第三方系统 ─┘      ↓                   ↓
              b2bwebsocket         metadata.odoo
                  ↓                   ↓
              b2brest            timedata.influx
                                      ↓
                                  InfluxDB
```
