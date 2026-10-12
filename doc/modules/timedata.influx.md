# io.openems.backend.timedata.influx

> Backend 时序数据存储 — InfluxDB 实现，OpenEMS 原生默认存储。

---

## 模块职责

Backend 侧的时序数据存储层，负责：
- 接收所有 Edge 上报的测点数据
- 批量写入 InfluxDB
- 提供历史数据查询接口（给前端曲线）
- 数据降采样

---

## 核心类

| 类 | 说明 |
|---|---|
| `InfluxTimeseries` | 时序存储实现类（OSGi 组件） |
| `Config` | 配置类 |
| `InfluxWriteWorker` | 批量写入线程 |
| `InfluxReadWorker` | 查询接口 |

---

## 数据模型

### InfluxDB Measurement 结构

```
Measurement: edge
Tags:
  - edgeId      设备 ID
  - channelAddress  通道地址 (ComponentId/ChannelId)

Fields:
  - value      数值
  - type       类型 (integer/float/boolean/...)

Time:
  - timestamp  毫秒时间戳
```

### 示例

```
measurement = "edge"
tags = edgeId="ess0", channelAddress="Ess(0)/ActivePower"
fields = value=5000, type="LONG"
time = 2026-09-18T10:00:00.000Z
```

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `url` | http://localhost:8086 | InfluxDB 地址 |
| `org` | openems | 组织名 (InfluxDB 2.x) |
| `bucket` | openems | 桶名 |
| `token` | — | 访问 Token |
| `isReadOnly` | false | 只读模式（不写入） |
| `threadPoolSize` | 2 | 写入线程数 |

---

## 写入流程

```
Edge 上报数据 (每 Cycle)
        ↓
Backend Edge Manager 接收
        ↓
InfluxTimeseries.write()
        ↓
内存缓冲区 (batch buffer)
        ↓
达到阈值 (时间/条数)
        ↓
InfluxWriteWorker 批量写入
        ↓
InfluxDB
```

### 批量写入策略

- **触发条件**：每 5 秒 或 每 1000 条
- **批量大小**：自动合并多个 Edge 的数据
- **失败重试**：失败后缓冲，下次重试

---

## 查询接口

### 历史曲线查询

```java
public SortedMap<ZonedDateTime, SortedMap<ChannelId, Object>> query(
    Edge edge,
    ChannelId[] channels,
    ZonedDateTime fromDate,
    ZonedDateTime toDate
)
```

- 前端大屏调用此接口获取历史曲线
- 支持降采样（自动按时间范围选精度）

---

## 与 Edge 侧的关系

```
Edge 侧:
  io.openems.edge.timedata.influxdb
    ↓ 本地写入 (可选)

Backend 侧:
  io.openems.backend.timedata.influx
    ↓ 云端聚合 (主要)

数据流向:
  Edge ──── 实时数据 ──── Backend ──── InfluxDB
       └─── (可选) 本地 InfluxDB ────
```

---

## 性能特点

| 指标 | 数值 | 说明 |
|---|---|---|
| 写入延迟 | < 100ms | 批量写入 |
| 查询延迟 | 秒级 | 单测点 1 天数据 |
| 压缩比 | ~10x | InfluxDB 自动压缩 |
| 保留策略 | 可配置 | 原始数据 N 天，降采样数据 M 天 |

---

## 与 IoTDB 对比

| 对比项 | InfluxDB | IoTDB |
|---|---|---|
| **生态** | 成熟，社区大 | 国产，工业场景多 |
| **写入性能** | 好 | 更好（时序优化） |
| **查询性能** | 好 | 高聚合更好 |
| **Schema** | 无模式 | 结构模型 |
| **运维** | 简单 | 较复杂 |
| **License** | 开源 | 开源 |
