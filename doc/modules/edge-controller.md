# Edge Controller 控制器

Controller 是 OpenEMS 的"大脑"，实现各种能源控制策略。每个 Controller 是一个独立的 OSGi 组件，通过 Scheduler 调度执行。

---

## 储能控制器 (ESS Controller)

### 基础充放电控制

| 模块名 | 功能 |
|---|---|
| `controller.ess.fixactivepower` | 固定有功功率充放电 |
| `controller.ess.fixstateofcharge` | 固定 SOC 目标 |
| `controller.ess.balancing` | 自发自用（功率平衡） |
| `controller.ess.cycle` | 充放电循环模式 |

### 电网交互控制

| 模块名 | 功能 |
|---|---|
| `controller.ess.selltogridlimit` | 上网功率限制 |
| `controller.ess.gridoptimizedcharge` | 电网优化充电 |
| `controller.ess.delaycharge` | 延时充电 |
| `controller.ess.delayedselltogrid` | 延时上网 |
| `controller.ess.hybrid.surplusfeedtogrid` | 多余电量上网 |

### 保护与安全

| 模块名 | 功能 |
|---|---|
| `controller.ess.emergencycapacityreserve` | 应急容量保留 |
| `controller.ess.limittotaldischarge` | 总放电量限制 |
| `controller.ess.mindischargeperiod` | 最小放电间隔 |
| `controller.ess.standby` | 待机模式 |
| `controller.ess.acisland` | 离网检测与切换 |
| `controller.ess.limiter14a` | 14A 限制（家用充电桩） |

### 高级功能

| 模块名 | 功能 |
|---|---|
| `controller.ess.timeofusetariff` | 分时电价优化 |
| `controller.ess.fastfrequencyreserve` | 快速调频 (FFR) |
| `controller.ess.reactivepowervoltagecharacteristic` | 无功-电压特性曲线 |
| `controller.ess.activepowervoltagecharacteristic` | 有功-电压特性曲线 |
| `controller.ess.linearpowerband` | 线性功率带 |
| `controller.ess.sohcycle` | SOH 循环管理 |
| `controller.ess.ripplecontrolreceiver` | 纹波控制接收器 |

---

## 充电桩控制器 (EVCS Controller)

| 模块名 | 功能 |
|---|---|
| `controller.evcs` | 充电桩总控 |
| `controller.evcs.fixactivepower` | 固定功率充电 |

---

## 不对称控制 (Asymmetric)

| 模块名 | 功能 |
|---|---|
| `controller.asymmetric.balancingcosphi` | 功率因数平衡 |
| `controller.asymmetric.fixreactivepower` | 固定无功功率 |
| `controller.asymmetric.peakshaving` | 不对称削峰 |
| `controller.asymmetric.phaserectification` | 相位校正 |

---

## 对称控制 (Symmetric)

| 模块名 | 功能 |
|---|---|
| `controller.symmetric.fixreactivepower` | 固定无功功率 |
| `controller.symmetric.peakshaving` | 削峰填谷 |
| `controller.symmetric.balancingschedule` | 平衡调度 |
| `controller.symmetric.limitactivepower` | 有功功率限制 |
| `controller.symmetric.randompower` | 随机功率（测试用） |
| `controller.symmetric.timeslotpeakshaving` | 分时段削峰 |

---

## IO 控制器

| 模块名 | 功能 |
|---|---|
| `controller.io.alarm` | 告警继电器 |
| `controller.io.analog` | 模拟量输出 |
| `controller.io.channelsinglethreshold` | 单通道阈值触发 |
| `controller.io.channelfixedoutput` | 固定数字输出 |
| `controller.io.heating.room` | 房间加热 |
| `controller.io.heatingelement` | 加热元件 |
| `controller.io.heatpump.sgready` | 热泵 SG-Ready |

---

## 其他控制器

| 模块名 | 功能 |
|---|---|
| `controller.channelthreshold` | 通道阈值告警 |
| `controller.chp.soc` | CHP 热电联产 SOC 控制 |
| `controller.cleverpv` | Clever PV 控制 |
| `controller.highloadtimeslot` | 高负荷时段 |
| `controller.generic.jsonlogic` | JSON 逻辑规则引擎 |
| `controller.pvinverter.fixpowerlimit` | 光伏逆变器功率限制 |
| `controller.pvinverter.selltogridlimit` | 光伏上网限制 |
| `controller.debug.log` | 调试日志 |
| `controller.debug.detailedlog` | 详细调试日志 |

---

## Controller 执行周期

```
每个 OpenEMS Cycle (默认 1 秒):
  1. TOPIC_CYCLE_BEFORE_PROCESS_IMAGE  预处理
  2. Scheduler 按顺序调用所有 Controller
     - 每个 Controller 读取 Channel 值
     - 计算控制策略
     - 写入目标值到 Channel
  3. TOPIC_CYCLE_EXECUTE_WRITE        执行写入
     - 设备驱动把 Channel 值翻译成实际指令下发
```
