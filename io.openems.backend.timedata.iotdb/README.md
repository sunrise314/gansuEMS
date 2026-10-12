# io.openems.backend.timedata.iotdb

> **[新增]** Apache IoTDB 时序数据存储适配层，替换原生 InfluxDB。

---

## 模块职责

实现 OpenEMS 原生 `Timedata` 接口，作为 IoTDB 存储后端：
- 接收 Edge 上报数据，批量写入 IoTDB
- 提供历史数据查询接口
- 支持与 InfluxDB 双写/切换

---

## 核心类

| 类 | 说明 |
|---|---|
| `IoTDbTimeseries` | 时序存储实现类（OSGi 组件） |
| `Config` | OSGi 配置类 |
| `IoTDbWriteWorker` | 批量写入线程 |
| `SessionPool` | IoTDB Session 连接池 |

---

## 存储模型

### IoTDB 路径结构

```
root.ems
  ├── {edgeId}              每个 Edge 一个节点
  │   ├── {componentId}_{channelId}    测点
  │   │   ├── value         数值
  │   │   └── type          类型
  │   └── ...
  └── ...
```

### 示例

```
root.ems.ess0_ActivePower
  root.ems.ess0_Soc
  root.ems.meter0_ActivePower
  root.ems.meter0_Voltage
```

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `ip` | 127.0.0.1 | IoTDB 节点 IP |
| `port` | 6667 | IoTDB RPC 端口 |
| `username` | root | 用户名 |
| `password` | root | 密码 |
| `storageGroup` | root.ems | 存储组 |
| `isReadOnly` | false | 只读模式 |
| `threadPoolSize` | 2 | 写入线程数 |
| `batchSize` | 1000 | 批量写入条数 |
| `flushInterval` | 5 | 刷新间隔（秒） |

---

## 写入流程

```
Edge 上报数据
     ↓
IoTDbTimeseries.write()
     ↓
内存 Tablet 缓冲区
     ↓
达到 batchSize 或 flushInterval
     ↓
SessionPool 取连接
     ↓
Tablet 批量插入
     ↓
IoTDB
```

### 批量写入优化

- **Tablet 格式**：列式存储，比一行一行插入快 10 倍以上
- **自动对齐时间戳**：同一时间戳的多个测点合并
- **失败重试**：写入失败后重新放入缓冲区

---

## 查询接口

### 历史曲线查询

```java
// SQL 示例
SELECT value FROM root.ems.ess0_ActivePower
WHERE time >= 2026-09-18T00:00:00
  AND time <= 2026-09-18T23:59:59
```

### 降采样查询

```sql
-- 按 1 分钟聚合
SELECT AVG(value) FROM root.ems.ess0_ActivePower
GROUP BY ([2026-09-18, 2026-09-19), 1m)
```

---

## 与 InfluxDB 双写策略

```
写入:
  Edge 数据
    ├──→ InfluxDB (原有)
    └──→ IoTDB (新增)

查询:
  前端查询 → 配置开关选 InfluxDB 或 IoTDB
```

**切换方式**：OSGi 配置界面修改 `activeStorage` 参数。

---

## 性能预期

| 指标 | InfluxDB | IoTDB (预期) |
|---|---|---|
| 写入吞吐 | 10k 点/秒 | 50k+ 点/秒 |
| 查询延迟 (1天数据) | 1-2 秒 | < 1 秒 |
| 压缩比 | 10x | 15-20x |
| 单测点存储成本 | 基准 | 降低 30-50% |

---

## 依赖关系

```
io.openems.backend.timedata.iotdb
  ↓ 依赖
io.openems.backend.common
org.apache.iotdb:iotdb-session
```

---

## 为什么换 IoTDB？

1. **写入性能更高**：工业场景每秒几十万测点，IoTDB 优化更好
2. **列式存储**：列式压缩比更高，存储成本更低
3. **中文生态好**：国产开源，文档和社区中文支持好
4. **工业场景验证**：电力、能源行业大规模使用
