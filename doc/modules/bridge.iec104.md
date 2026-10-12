# io.openems.edge.bridge.iec104

> **[新增]** IEC 60870-5-104 电力远动协议通信桥，对接场站 RTU。

---

## 模块职责

作为 IEC104 通信桥，负责：
- 与 RTU（远程终端单元）建立 TCP 连接
- 接收遥测（YC）、遥信（YX）
- 下发遥控（YK）、遥调（YT）指令
- 断线自动重连
- 周期总召唤

---

## 核心类

| 类 | 说明 |
|---|---|
| `BridgeIec104TcpImpl` | TCP 实现类（OSGi 组件） |
| `AbstractIec104Bridge` | 桥接器抽象基类 |
| `AbstractOpenemsIec104Component` | 设备组件抽象基类 |
| `Iec104Protocol` | 点位映射定义 |
| `TeleValue` / `TeleSignal` | 遥测/遥信映射 DTO |
| `Iec104WriteRequest` | 写请求工厂 |
| `ConfigTcp` | OSGi 配置类 |

---

## IEC104 协议基础

| 类型 | ASDU Type | 说明 |
|---|---|---|
| **遥测 (YC)** | M_ME_NC_1 (13) | 短浮点测量值 |
| **遥信 (YX)** | M_SP_NA_1 (1) | 单点信息 |
| **遥控 (YK)** | C_SC_NA_1 (45) | 单命令 |
| **遥调 (YT)** | C_SE_NC_1 (50) | 短浮点设定值 |
| **总召唤** | C_IC_NA_1 (100) | 全数据同步 |

---

## 工作原理

### 1. 连接建立

```
Client (OpenEMS) ──── TCP ──── RTU (Server)
                      port 2404 (标准)
```

超时参数：
- **t0** (30s)：连接建立超时
- **t1** (15s)：发送/确认超时
- **t2** (10s)：确认延迟
- **t3** (20s)：空闲测试超时

### 2. 数据流向

```
上行（遥测/遥信）：
  RTU ──── ASDU ──── BridgeIec104TcpImpl
                         ↓ 解析 IOA + 值
                    查找点位映射
                         ↓
                    更新设备 Channel

下行（遥控/遥调）：
  Controller ──── 写 Channel ──── 设备驱动
                                      ↓ 翻译为 ASDU
                                BridgeIec104TcpImpl
                                      ↓ 发送
                                  RTU
```

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `ip` | — | RTU IP 地址 |
| `port` | 2404 | 标准 IEC104 端口 |
| `asduCommonAddress` | 1 | ASDU 公共地址 |
| `t0` | 30 | 连接超时（秒） |
| `t1` | 15 | 确认超时（秒） |
| `t2` | 10 | 确认延迟（秒） |
| `t3` | 20 | 空闲测试（秒） |
| `usePeriodicInterrogation` | true | 是否周期总召唤 |
| `interrogationInterval` | 60 | 总召唤间隔（秒） |

---

## 依赖关系

```
RTU 设备驱动 (rtu.iec104)
  ↓ 使用
BridgeIec104 接口
  ↓ 依赖
io.openems.edge.common
j60870 (IEC104 库)
```

---

## 与 Modbus 对比

| 对比项 | Modbus | IEC104 |
|---|---|---|
| **行业** | 工业自动化通用 | 电力系统专用 |
| **通信方式** | 请求-响应 | 主动上送 + 请求 |
| **数据模型** | 寄存器映射 | 信息对象地址 (IOA) |
| **应用层** | 简单 | 复杂（COT、ASDU 类型） |
| **实时性** | 轮询 | 事件驱动 + 周期 |
| **典型设备** | 电表/逆变器 | 场站 RTU/保护装置 |

---

## 使用示例

```java
// 1. 配置 Bridge
// OSGi 界面创建 "Bridge IEC104/TCP"
// 填入 RTU IP = 192.168.1.100, port = 2404

// 2. 配置设备
// OSGi 界面创建 "RTU IEC104"
// 填入 bridge_id = "iec104bridge0"
// 填入各 IOA 地址

// 3. 代码中使用
rtu.sendAgcActivePowerSetpoint(50000); // 下发 AGC 50kW
```
