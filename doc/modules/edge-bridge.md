# Edge Bridge 通信桥

Bridge 是 Edge 层的通信协议适配层。设备驱动通过 Bridge 连接到物理设备，Bridge 负责协议编解码、连接管理、读写重试。

---

## 通信桥模块

| 模块名 | 协议 | 说明 |
|---|---|---|
| `io.openems.edge.bridge.modbus` | Modbus TCP/RTU | 最常用的工业通信协议，电表、逆变器都用这个 |
| `io.openems.edge.bridge.mqtt` | MQTT | 物联网消息协议，云平台对接用 |
| `io.openems.edge.bridge.http` | HTTP/REST | HTTP 接口调用，第三方 API 对接 |
| `io.openems.edge.bridge.mbus` | M-Bus | 仪表总线，热量表、水表常用 |
| `io.openems.edge.bridge.onewire` | 1-Wire | 单总线协议，温度传感器用 |
| `io.openems.edge.bridge.iec104` | **[新增]** IEC 60870-5-104 | 电力远动协议，对接场站 RTU |

---

## Bridge 架构模式

```
设备驱动 (meter.eastron / battery.pylontech ...)
    ↓ 使用
Bridge 接口 (BridgeModbus / BridgeIec104 ...)
    ↓ 实现
Bridge 实现 (BridgeModbusTcpImpl / BridgeIec104TcpImpl ...)
    ↓ 调用
硬件设备 (电表/逆变器/RTU ...)
```

### 关键设计点

1. **Bridge 与设备分离**：一个 Bridge 可以被多个设备驱动共享
2. **协议编解码在 Bridge 层**：设备驱动只关心业务语义，不关心字节流
3. **连接管理在 Bridge 层**：断线重连、超时、重试都在 Bridge 实现
4. **Channel 映射在设备层**：IOA/寄存器地址 → OpenEMS Channel 的映射由设备驱动定义

---

## 典型使用流程

```
1. 配置 Bridge：创建 Bridge IEC104/TCP，填入 RTU IP、端口
2. 配置设备：创建 RTU 设备，指定 bridge_id 和点位映射
3. 运行时：
   - Bridge 负责连接 RTU、接收数据
   - 设备驱动根据点位映射把数据写入 Channel
   - Controller 从 Channel 读数据，执行控制算法
   - Controller 写指令到 Channel，设备驱动翻译后通过 Bridge 下发
```
