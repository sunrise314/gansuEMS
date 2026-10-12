# io.openems.edge.bridge.modbus

> Modbus TCP/RTU 通信桥，OpenEMS 最常用的设备通信协议。

---

## 模块职责

作为通信桥，负责：
- Modbus TCP/RTU 连接管理
- 寄存器读写
- 断线重连
- 协议编解码

设备驱动（电表、逆变器、电池）通过它和物理设备通信。

---

## 核心类

| 类 | 说明 |
|---|---|
| `BridgeModbusTcpImpl` | Modbus TCP 实现类（OSGi 组件） |
| `BridgeModbusSerialImpl` | Modbus RTU（串口）实现类 |
| `AbstractModbusBridge` | 桥接器抽象基类 |
| `AbstractOpenemsModbusComponent` | 设备组件抽象基类 |
| `ModbusProtocol` | 协议定义（寄存器映射） |
| `ConfigTcp` / `ConfigSerial` | 配置类 |

---

## 工作原理

### 1. 连接管理

```
BridgeModbusTcpImpl
  ├── 维护 TCP 连接
  ├── 超时/重试配置
  └── 断线自动重连
```

### 2. 设备驱动模式

```java
// 设备驱动继承 AbstractOpenemsModbusComponent
public class MeterEastronSdm630Impl extends AbstractOpenemsModbusComponent
        implements ElectricityMeter {

    @Override
    protected void execute(ModbusSlave ...) {
        // 定义寄存器映射
        this.modbusProtocol = new ModbusProtocol(this,
            new ReadCoilTask(...),
            new WriteRegTask(...)
        );
    }
}
```

### 3. 寄存器映射

```java
// 每个寄存器对应一个 Channel
this.bind(ChannelId.ACTIVE_POWER, //
    new ElementToChannelElement<>(new Int32Element(40001), //
        ElementToChannelConverter.SCALE_FACTOR_3)); // 缩放因子
```

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `ip` | — | 设备 IP 地址 |
| `port` | 502 | Modbus 标准端口 |
| `unitId` | 1 | Modbus 从站地址 |
| `invalidateElementsAfterReadErrors` | 1 | 读错误多少次后失效 |
| `logVerbosity` | NONE | 日志级别 |

---

## 依赖关系

```
设备驱动 (meter.*/battery.*/ess.*)
  ↓ 使用
BridgeModbus 接口
  ↓ 依赖
io.openems.edge.common
io.openems.common
j2mod (Modbus 库)
```

---

## 典型应用场景

| 场景 | 说明 |
|---|---|
| 电表读取 | 所有 Eastron/ABB/西门子电表都通过 Modbus |
| 电池 BMS | Pylontech/Soltaro 电池包 |
| 储能逆变器 | 大部分储能变流器 |
| 充电桩 | KEBA/Mennekes 充电柱 |

---

## 为什么用 Bridge 模式？

1. **一个 Bridge 多个设备**：多个设备共用一个 TCP 连接
2. **协议复用**：Modbus 编解码逻辑只写一遍
3. **设备驱动简洁**：设备驱动只关心寄存器→Channel 映射
4. **统一错误处理**：连接断开、超时、重试都在 Bridge 层处理
