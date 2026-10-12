# OpenEMS 模块总览索引

> 本文档列出 OpenEMS 所有模块的功能说明，按类别分组。

## 模块总数

| 层级 | 类别 | 模块数 | 说明 |
|---|---|---|---|
| **Backend** | 云后端 | 21 | 聚合、监控、远程控制 |
| **Edge - Bridge** | 通信桥 | 6 | 设备通信协议适配 |
| **Edge - Meter** | 电表 | 23 | 各类电表设备驱动 |
| **Edge - Battery** | 电池 | 7 | 电池管理系统(BMS)驱动 |
| **Edge - ESS** | 储能系统 | 10 | 储能变流器/系统 |
| **Edge - PvInverter** | 光伏逆变器 | 5 | 光伏逆变器驱动 |
| **Edge - EVCS/EVSE** | 充电桩 | 24 | 电动汽车充电站驱动 |
| **Edge - IO** | 输入输出 | 10 | 数字/模拟 IO 模块 |
| **Edge - Controller** | 控制器 | 58 | 能源控制算法 |
| **Edge - Scheduler** | 调度器 | 5 | 控制器执行顺序调度 |
| **Edge - Predictor** | 预测器 | 6 | 负荷/发电预测 |
| **Edge - TimeOfUseTariff** | 分时电价 | 13 | 电价数据源 |
| **Edge - 其他** | 温度/天气/时序 | 10 | 辅助功能 |
| **Common/Core** | 公共模块 | 5 | 公共 API 和工具 |
| **Wrapper** | 第三方包装 | 2 | 非 OSGi 库包装 |

---

## 分类文档

| 分类 | 文档 |
|---|---|
| Backend 模块 | [backend.md](./backend.md) |
| Edge Bridge 通信桥 | [edge-bridge.md](./edge-bridge.md) |
| Edge Meter 电表 | [edge-meter.md](./edge-meter.md) |
| Edge Battery/ESS 储能 | [edge-ess.md](./edge-ess.md) |
| Edge Controller 控制器 | [edge-controller.md](./edge-controller.md) |
| Edge EVCS/EVSE 充电桩 | [edge-evcs.md](./edge-evcs.md) |
| Edge IO/其他设备 | [edge-misc.md](./edge-misc.md) |

---

## 架构分层图

```
┌─────────────────────────────────────────────────────────┐
│  UI (Angular/Ionic)                                     │
└───────────────────────────┬─────────────────────────────┘
                            │ WebSocket / REST
┌───────────────────────────▼─────────────────────────────┐
│  Backend (云端)                                          │
│  ├── edge.manager      管理所有 Edge                    │
│  ├── metadata.odoo     元数据管理 (Odoo)                │
│  ├── timedata.influx   时序数据 (InfluxDB)             │
│  ├── b2bwebsocket      B2B WebSocket 接口              │
│  ├── uiwebsocket       UI WebSocket 接口               │
│  ├── alerting          告警服务                         │
│  └── authentication    OAuth2 认证                      │
└───────────────────────────┬─────────────────────────────┘
                            │ Edge-Cloud 协议
┌───────────────────────────▼─────────────────────────────┐
│  Edge (现场)                                             │
│  ├── core              Edge 核心组件                    │
│  ├── common            公共 API                         │
│  │                                                       │
│  ├── Bridge 通信桥                                       │
│  │   ├── bridge.modbus      Modbus TCP/RTU              │
│  │   ├── bridge.mqtt        MQTT                        │
│  │   ├── bridge.http        HTTP                        │
│  │   ├── bridge.mbus        M-Bus                       │
│  │   └── bridge.onewire     1-Wire                      │
│  │                                                       │
│  ├── 设备驱动 (通过 Bridge 连接)                        │
│  │   ├── meter.*         电表 (23种)                    │
│  │   ├── battery.*       电池 BMS (7种)                 │
│  │   ├── ess.*           储能系统 (10种)                │
│  │   ├── pvinverter.*    光伏逆变器 (5种)               │
│  │   ├── evcs.*          充电桩 (15种)                  │
│  │   └── io.*            IO 模块 (10种)                 │
│  │                                                       │
│  ├── Controller 控制器 (58种)                           │
│  │   ├── controller.ess.*    储能控制策略               │
│  │   ├── controller.evcs.*   充电桩控制                 │
│  │   └── controller.io.*     IO 控制                    │
│  │                                                       │
│  └── Scheduler 调度器                                    │
│      ├── scheduler.daily     按日调度                   │
│      └── scheduler.fixedorder 固定顺序                   │
└─────────────────────────────────────────────────────────┘
```
