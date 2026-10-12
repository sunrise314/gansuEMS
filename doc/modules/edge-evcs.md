# Edge 充电桩模块 (EVCS / EVSE)

电动汽车充电站相关设备驱动。

---

## EVCS 充电站

| 模块名 | 品牌/型号 | 说明 |
|---|---|---|
| `io.openems.edge.evcs.api` | — | 充电站公共 API |
| `io.openems.edge.evcs.core` | — | 充电站核心组件 |
| `io.openems.edge.evcs.cluster` | — | 多充电站簇管理 |
| `io.openems.edge.evcs.abl` | ABL | ABL 充电桩 |
| `io.openems.edge.evcs.goe` | go-e | go-eCharger |
| `io.openems.edge.evcs.goe.chargerhome` | go-e | go-e Charger Home |
| `io.openems.edge.evcs.heidelberg` | Heidelberg | Heidelberg 充电桩 |
| `io.openems.edge.evcs.openwb` | openWB | openWB 充电桩 |
| `io.openems.edge.evcs.spelsberg` | Spelsberg | Spelsberg 充电桩 |
| `io.openems.edge.evcs.webasto.next` | Webasto | Webasto Next |
| `io.openems.edge.evcs.webasto.unite` | Webasto | Webasto Unite |
| `io.openems.edge.evcs.ocpp.server` | OCPP Server | OCPP 服务端（作为充电桩） |
| `io.openems.edge.evcs.ocpp.abl` | ABL OCPP | ABL OCPP 充电桩 |
| `io.openems.edge.evcs.ocpp.common` | OCPP Common | OCPP 公共组件 |
| `io.openems.edge.evcs.ocpp.ies.keywatt.singleccs` | IES Keywatt | IES Keywatt 单 CCS |

---

## EVSE 充电接口

| 模块名 | 品牌 | 说明 |
|---|---|---|
| `io.openems.edge.evse.api` | — | 充电接口公共 API |
| `io.openems.edge.evse.chargepoint.alfen` | Alfen | Alfen 充电柱 |
| `io.openems.edge.evse.chargepoint.alpitronic` | Alpitronic | Alpitronic 快充桩 |
| `io.openems.edge.evse.chargepoint.bender` | Bender | Bender 充电柱 |
| `io.openems.edge.evse.chargepoint.hardybarth` | Hardy Barth | Hardy Barth |
| `io.openems.edge.evse.chargepoint.heidelberg` | Heidelberg | Heidelberg |
| `io.openems.edge.evse.chargepoint.keba` | KEBA | KEBA 充电柱 |
| `io.openems.edge.evse.chargepoint.mennekes | Mennekes | Mennekes |
| `io.openems.edge.evse.electricvehicle.generic` | Generic | 通用电动车模型 |

---

## OCPP 协议说明

OCPP (Open Charge Point Protocol) 是充电桩行业标准协议：
- `evcs.ocpp.server`：OpenEMS 作为 OCPP Server，充电桩作为 Client 连接上来
- `evcs.ocpp.xxx`：OpenEMS 作为 Client，连接第三方 OCPP Server

---

## 标准 Channel (Evcs)

| Channel | 说明 |
|---|---|
| `Status` 状态 | 空闲/充电/错误 |
| `Power` 充电功率 (W) | 当前充电功率 |
| `EnergySession` 本次充电量 (Wh) | 本次会话充入电量 |
| `EnergyTotal` 累计充电量 (Wh) | 总累计 |
| `ActivePower` 有功功率 | |
| `MaxPower` 最大功率 (W) | 可设置的最大功率 |
