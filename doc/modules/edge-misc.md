# Edge 其他设备与功能模块

---

## IO 输入输出模块

| 模块名 | 品牌/类型 | 说明 |
|---|---|---|
| `io.openems.edge.io.api` | — | IO 公共 API |
| `io.openems.edge.io.filipowski` | Filipowski | IO 模块 |
| `io.openems.edge.io.gpio` | 树莓派 GPIO | 树莓派通用 IO |
| `io.openems.edge.io.kmtronic` | KMTronic | Modbus IO 模块 |
| `io.openems.edge.io.offgridswitch` | 离网开关 | 离网模式切换开关 |
| `io.openems.edge.io.revpi` | RevPi | Revolution Pi IO |
| `io.openems.edge.io.shelly` | Shelly | Shelly 智能开关 |
| `io.openems.edge.io.siemenslogo` | Siemens Logo | 西门子 Logo PLC |
| `io.openems.edge.io.wago` | WAGO | WAGO IO 模块 |
| `io.openems.edge.io.weidmueller` | Weidmüller | 魏德米勒 IO |

---

## 调度器 (Scheduler)

调度器决定所有 Controller 的执行顺序。

| 模块名 | 策略 | 说明 |
|---|---|---|
| `io.openems.edge.scheduler.api` | — | 调度器公共 API |
| `io.openems.edge.scheduler.fixedorder` | 固定顺序 | 按配置的固定顺序执行 |
| `io.openems.edge.scheduler.daily` | 按日 | 按一天中的时间段切换顺序 |
| `io.openems.edge.scheduler.allalphabetically` | 字母序 | 按字母顺序执行（调试用） |
| `io.openems.edge.scheduler.jscalendar` | JS 日历 | 用 JavaScript 定义调度日历 |

---

## 预测器 (Predictor)

预测负荷和发电曲线，用于优化控制策略。

| 模块名 | 算法 | 说明 |
|---|---|---|
| `io.openems.edge.predictor.api` | — | 预测器公共 API |
| `io.openems.edge.predictor.persistencemodel` | 持久化模型 | 基于历史数据的模型 |
| `io.openems.edge.predictor.similardaymodel` | 相似日 | 找历史相似日预测 |
| `io.openems.edge.predictor.production.linearmodel` | 线性模型 | 线性回归预测 |
| `io.openems.edge.predictor.profileclusteringmodel` | 聚类 | 曲线聚类预测 |
| `io.openems.edge.predictor.lstm` | LSTM | 深度学习 LSTM 预测 |

---

## 分时电价 (TimeOfUseTariff)

| 模块名 | 数据源 | 说明 |
|---|---|---|
| `io.openems.edge.timeofusetariff.api` | — | 电价公共 API |
| `io.openems.edge.timeofusetariff.awattar` | aWATTar | 德国 aWATTar 实时电价 |
| `io.openems.edge.timeofusetariff.tibber` | Tibber | Tibber 实时电价 |
| `io.openems.edge.timeofusetariff.entsoe` | ENTSO-E | 欧洲电力交易所 |
| `io.openems.edge.timeofusetariff.corrently` | Corrently | Corrently 电价 |
| `io.openems.edge.timeofusetariff.swisspower` | Swiss Power | 瑞士电力 |
| `io.openems.edge.timeofusetariff.hassfurt | Hassfurt | Hassfurt 电价 |
| `io.openems.edge.timeofusetariff.ews` | EWS | EWS 电价 |
| `io.openems.edge.timeofusetariff.groupe | Groupe | Groupe 电价 |
| `io.openems.edge.timeofusetariff.ancillarycosts` | 辅助成本 | 辅助服务费计算 |
| `io.openems.edge.timeofusetariff.rabotcharge` | Rabot Charge | Rabot 智能电价 |
| `io.openems.edge.timeofusetariff.manual` | 手动 | 手动配置电价表 |
| `io.openems.edge.timeofusetariff.luox | Luox | Luox 电价 |

---

## 天气与温度

| 模块名 | 功能 |
|---|---|
| `io.openems.edge.weather.api` | 天气公共 API |
| `io.openems.edge.weather.openmeteo` | Open-Meteo 天气数据 |
| `io.openems.edge.thermometer.api` | 温度计公共 API |
| `io.openems.edge.onewire.thermometer` | 1-Wire 温度传感器 |

---

## 其他设备

| 模块名 | 说明 |
|---|---|
| `io.openems.edge.fronius` | Fronius 逆变器（集成型） |
| `io.openems.edge.huawei` | 华为逆变器（集成型） |
| `io.openems.edge.sma` | SMA 逆变器（集成型） |
| `io.openems.edge.solaredge` | SolarEdge 逆变器 |
| `io.openems.edge.goodwe` | 固德威逆变器 |
| `io.openems.edge.kostal` | Kostal 逆变器 |
| `io.openems.edge.kaco.blueplanet.hybrid10` | KACO 混合逆变器 |
| `io.openems.edge.victron` | Victron 储能系统 |
| `io.openems.edge.braiinsos` | Braiins OS（比特币挖矿） |
| `io.openems.edge.katek.edcom` | KATEK EDCOM |
| `io.openems.edge.protectionrelay.telehaase` | Telehaase 保护继电器 |
| `io.openems.edge.edge2edge` | Edge 间通信 |
| `io.openems.edge.energy` | 能量管理核心 |
| `io.openems.edge.energy.api` | 能量管理 API |
| `io.openems.edge.simulator` | Edge 模拟器 |
| `io.openems.edge.rtu.iec104` | **[新增]** IEC104 RTU 设备 |

---

## 公共与工具模块

| 模块名 | 说明 |
|---|---|
| `io.openems.common` | 公共工具类、类型、异常 |
| `io.openems.edge.common` | Edge 公共 API、组件基类 |
| `io.openems.edge.core` | Edge 核心组件 |
| `io.openems.backend.common` | Backend 公共 API |
| `io.openems.core.logger` | 日志核心 |
| `io.openems.core.referencetarget` | 引用目标工具 |
| `io.openems.shared.influxdb` | InfluxDB 共享工具 |
| `io.openems.wrapper` | 第三方库 OSGi 包装 |
