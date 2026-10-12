# io.openems.edge.rtu.iec104

> **[新增]** IEC104 RTU 设备驱动，对接场站远程终端单元，实现 AGC 功率指令下发。

---

## 模块职责

作为 RTU 设备驱动，负责：
- 对接 `bridge.iec104` 通信桥
- 定义点位映射（IOA ↔ OpenEMS Channel）
- 接收 RTU 上送的遥测遥信
- 提供 AGC 功率下发、并离网控制 API

---

## 核心类

| 类 | 说明 |
|---|---|
| `RtUImpl` | RTU 设备实现类（OSGi 组件） |
| `RtUConfig` | OSGi 配置类 |
| `ChannelId` | Channel 枚举定义 |

---

## Channel 定义

### 遥测（YC，只读）

| Channel | IOA 范围 | 类型 | 说明 |
|---|---|---|---|
| `ActivePower` | 1-100 | Float | 总有功功率 (W) |
| `ReactivePower` | | Float | 总无功功率 (var) |
| `VoltageL1` | | Float | A 相电压 (V) |
| `VoltageL2` | | Float | B 相电压 (V) |
| `VoltageL3` | | Float | C 相电压 (V) |
| `CurrentL1` | | Float | A 相电流 (A) |
| `CurrentL2` | | Float | B 相电流 (A) |
| `CurrentL3` | | Float | C 相电流 (A) |
| `Frequency` | | Float | 电网频率 (Hz) |
| `Soc` | | Float | 电池 SOC (%) |
| `Temperature` | | Float | 电池温度 (°C) |

### 遥信（YX，只读）

| Channel | 类型 | 说明 |
|---|---|---|
| `GridConnected` | Boolean | 并网状态 |
| `EmergencyStop` | Boolean | 急停状态 |
| `FaultCode` | Integer | 故障码 |

### 遥调/遥控（YT/YK，可写）

| Channel | IOA 范围 | 类型 | 说明 |
|---|---|---|---|
| `AgcActivePowerSetpoint` | 1000-1100 | Float | AGC 有功功率设定 (W) |
| `AgcReactivePowerSetpoint` | | Float | AGC 无功功率设定 (var) |
| `ConnectGrid` | 2000-2010 | Boolean | 并网/离网控制 |

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `bridgeId` | — | 关联的 IEC104 Bridge ID |
| `asduAddress` | 1 | ASDU 公共地址 |
| `ioaActivePower` | 1 | 有功功率遥测 IOA |
| `ioaReactivePower` | 2 | 无功功率遥测 IOA |
| `ioaVoltageL1` | 3 | A 相电压 IOA |
| ... | ... | ... |
| `ioaAgcActivePowerSetpoint` | 1001 | AGC 有功设定 IOA |
| `ioaConnectGrid` | 2001 | 并网控制 IOA |

---

## 工作原理

### 上行（接收 RTU 数据）

```
RTU 上送 ASDU
    ↓
BridgeIec104 解析
    ↓
查找 IOA 映射
    ↓
更新 Channel:
  Rtu(0)/ActivePower = 5000
  Rtu(0)/Soc = 75.5
```

### 下行（下发指令）

```java
// Controller 调用
rtu.sendAgcActivePowerSetpoint(50000); // 50kW

// 内部流程:
1. 写入 Channel: AgcActivePowerSetpoint
2. 触发 Iec104WriteRequest
3. Bridge 发送 C_SE_NC_1 ASDU
4. RTU 执行功率调节
```

---

## AGC 控制流程

```
调度中心 (AGC)
    ↓ 下发指令 (经过 IEC104)
RTU 接收
    ↓
OpenEMS Edge
    ↓ 解析为 Channel
Controller 安全校验
    ├── 功率越限检查
    ├── SOC 上下限检查
    └── 时间窗口检查
    ↓
校验通过 → 下发到储能变流器
校验失败 → 拒绝 + 告警
```

---

## 与 Bridge 的关系

```
io.openems.edge.rtu.iec104
  ↓ 使用
io.openems.edge.bridge.iec104
  ↓ 连接
RTU 物理设备
```

**职责分离**：
- **Bridge**：只管连接和协议编解码
- **RTU 设备驱动**：管点位映射和业务语义

---

## 典型部署场景

```
场站侧:
  储能电站
    ├── 电池簇 1, 2, 3...
    ├── PCS 变流器
    ├── 电表
    └── RTU (远程终端单元)
          ↓ IEC104
        OpenEMS Edge
          ↓
        调度主站
```

---

## 依赖关系

```
io.openems.edge.rtu.iec104
  ↓ 依赖
io.openems.edge.common
io.openems.edge.bridge.iec104
```
