# Edge 储能系统模块 (Battery / ESS)

储能相关模块，包括电池 BMS、储能变流器、完整储能系统。

---

## Battery BMS 电池管理系统

| 模块名 | 品牌 | 说明 |
|---|---|---|
| `io.openems.edge.battery.api` | — | 电池公共 API 接口 |
| `io.openems.edge.battery.bmw` | BMW | BMW 电池包 |
| `io.openems.edge.battery.bydcommercial` | 比亚迪 | 商用电池柜 |
| `io.openems.edge.battery.fenecon.commercial` | FENECON | FENECON 商用储能 |
| `io.openems.edge.battery.fenecon.home` | FENECON | FENECON 家用储能 |
| `io.openems.edge.battery.pylontech` | 派能 | Pylontech 家用电池 |
| `io.openems.edge.battery.soltaro` | 硕日 | Soltaro 电池 |

---

## BatteryInverter 电池逆变器

| 模块名 | 品牌 | 说明 |
|---|---|---|
| `io.openems.edge.batteryinverter.api` | — | 电池逆变器公共 API |
| `io.openems.edge.batteryinverter.kaco.blueplanetgridsave` | KACO | KACO Blueplanet Grid Save |
| `io.openems.edge.batteryinverter.refu` | Refu | Refu 电池逆变器 |
| `io.openems.edge.batteryinverter.sinexcel` | Sinexcel | Sinexcel 逆变器 |

---

## ESS 储能系统 (Energy Storage System)

| 模块名 | 说明 |
|---|---|
| `io.openems.edge.ess.api` | 储能公共 API |
| `io.openems.edge.ess.core` | 储能核心组件 |
| `io.openems.edge.ess.cluster` | 多储能簇并联 |
| `io.openems.edge.ess.generic` | 通用储能（BMS + 逆变器分开配置） |
| `io.openems.edge.ess.samsung` | Samsung 储能系统 |
| `io.openems.edge.ess.adstec.storaxe` | Adstec Storaxe |
| `io.openems.edge.ess.fenecon.commercial40` | FENECON Commercial 40 |
| `io.openems.edge.tesla.powerwall2` | 特斯拉 Powerwall 2 |
| `io.openems.edge.bosch.bpts5hybrid` | Bosch BP TS 5 Hybrid |
| `io.openems.edge.victron` | Victron 储能系统 |

---

## 储能系统架构

```
完整储能系统 (ESS)
├── Battery BMS (电池管理系统)
│   ├── SOC 状态
│   ├── 电压/电流/温度
│   └── 故障告警
└── Battery Inverter (电池逆变器)
    ├── 充放电功率控制
    └── 并/离网切换
```

---

## 标准 Channel (ManagedSymmetricEss)

| Channel | 说明 |
|---|---|
| `ActivePower` 有功功率 (W) | 正=放电，负=充电 |
| `ReactivePower` 无功功率 (var) | |
| `Capacity` 容量 (Wh) | 电池总容量 |
| `Soc` 剩余电量 (%) | 0-100 |
| `MaxApparentPower` 最大视在功率 (VA) | |
| `AllowedChargePower` 最大充电功率 (W) | |
| `AllowedDischargePower` 最大放电功率 (W) | |
| `GridMode` 并网模式 | ON_GRID / OFF_GRID |
