# Edge Meter 电表模块

所有电表设备驱动，通过 Modbus 或其他协议读取电能数据。

---

## 电表驱动列表

| 模块名 | 品牌/型号 | 通信方式 |
|---|---|---|
| `io.openems.edge.meter.api` | — | 电表公共 API 接口 |
| `io.openems.edge.meter.abb` | ABB | Modbus |
| `io.openems.edge.meter.artemes.am2` | Artemes AM2 | Modbus |
| `io.openems.edge.meter.bcontrol.em300` | B-Control EM300 | Modbus |
| `io.openems.edge.meter.bgetech` | B.G.Tech | Modbus |
| `io.openems.edge.meter.camillebauer.aplus` | Camille Bauer APLUS | Modbus |
| `io.openems.edge.meter.carlo.gavazzi` | Carlo Gavazzi | Modbus |
| `io.openems.edge.meter.chint` | 正泰 | Modbus |
| `io.openems.edge.meter.discovergy` | Discovergy | HTTP API |
| `io.openems.edge.meter.eastron` | Eastron SDM120/SDM630 | Modbus |
| `io.openems.edge.meter.hager` | Hager | Modbus |
| `io.openems.edge.meter.janitza` | Janitza | Modbus |
| `io.openems.edge.meter.kdk` | KDK | Modbus |
| `io.openems.edge.meter.opendtu` | OpenDTU | HTTP API |
| `io.openems.edge.meter.phoenixcontact` | Phoenix Contact | Modbus |
| `io.openems.edge.meter.plexlog` | Plexlog | Modbus |
| `io.openems.edge.meter.pqplus` | PQ Plus | Modbus |
| `io.openems.edge.meter.schneider.acti9.smartlink` | 施耐德 Acti9 | Modbus |
| `io.openems.edge.meter.siemens` | 西门子 | Modbus |
| `io.openems.edge.meter.socomec` | Socomec | Modbus |
| `io.openems.edge.meter.virtual` | 虚拟电表 | 软件模拟 |
| `io.openems.edge.meter.weidmueller` | Weidmüller | Modbus |
| `io.openems.edge.meter.ziehl` | Ziehl | Modbus |

---

## 电表类型分类

| 类型 | 说明 | 典型应用 |
|---|---|---|
| **Grid 电表** | 并网点电表 | 计量并网功率、电网用电 |
| **Production 电表** | 光伏发电表 | 计量光伏发电量 |
| **Consumption 电表** | 负载电表 | 计量负荷用电量 |

---

## 公共 Channel

所有电表都实现 `ElectricityMeter` 接口，标准 Channel 包括：

- `ActivePower` 有功功率 (W)
- `ReactivePower` 无功功率 (var)
- `Voltage` 电压 (V)
- `Current` 电流 (A)
- `Frequency` 频率 (Hz)
- `ActiveProductionEnergy` 发电电能 (Wh)
- `ActiveConsumptionEnergy` 用电电能 (Wh)
