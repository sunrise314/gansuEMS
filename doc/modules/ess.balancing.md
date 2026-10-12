# io.openems.edge.controller.ess.balancing

> 储能核心控制器 — 自发自用（功率平衡）策略，家用储能最常用的控制算法。

---

## 模块职责

实现"自发自用"控制策略：
- 光伏发电优先供负荷使用
- 多余的电给电池充电
- 电池充满后，多余电量上网
- 负荷大于发电时，电池放电补充

**目标：最大化自发自用率，最小化从电网取电。**

---

## 控制算法

### 数学模型

```
净负荷 = 负荷 - 光伏
        ↓
  净负荷 > 0 (用电大于发电)
        ↓
   电池放电补充
   P_ess = 净负荷 (放电)

  净负荷 < 0 (发电大于用电)
        ↓
   电池充电
   P_ess = 净负荷 (充电，负值)
```

### 限制条件

```
P_ess_target = clamp(
    P_balance,           // 平衡功率
    -P_charge_max,       // 最大充电功率
    P_discharge_max      // 最大放电功率
)

同时受 SOC 约束：
  - SOC 低 → 减少放电
  - SOC 高 → 减少充电
```

---

## 核心类

| 类 | 说明 |
|---|---|
| `EssBalancingImpl` | 控制器实现类（OSGi 组件） |
| `Config` | 配置类 |

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `enabled` | true | 是否启用 |
| `ess_id` | ess0 | 储能组件 ID |
| `meter_id` | meter0 | 并网点电表 ID |
| `targetGridSetpoint` | 0 | 目标电网功率 (W)，0=完全平衡 |
| `maxChargePower` | 0 | 最大充电功率限制 (W)，0=不限 |
| `maxDischargePower` | 0 | 最大放电功率限制 (W)，0=不限 |
| `mode` | FORCE_CHARGE | 控制模式 |

---

## 数据流向

```
每个 Cycle (1秒):
  1. 读取 Grid Meter.ActivePower
  2. 读取 ESS.Soc
  3. 读取 ESS.AllowedChargePower / AllowedDischargePower
  4. 计算目标功率: P_target = GridSetpoint - GridActivePower
  5. 应用功率限制
  6. 应用 SOC 限制
  7. 写入 ESS.SetActivePower
  8. 设备驱动下发到储能变流器
```

---

## 与其他控制器的关系

```
优先级从高到低：
  1. EmergencyCapacityReserve  应急容量
  2. SellToGridLimit           上网限制
  3. Balancing                  自发自用（基础策略）
  4. FixActivePower            固定功率
```

Scheduler 按优先级执行，高优先级会覆盖低优先级的设定。

---

## 典型应用场景

| 场景 | 效果 |
|---|---|
| 白天光伏 > 负荷 | 多余电量给电池充电 |
| 晚上负荷 > 零发电 | 电池放电供负荷 |
| 阴雨天 | 电池从电网充电（如有需要） |
| 电池满了 | 多余电量上网 |

---

## 相关 Channel

| Channel | 来源 | 说明 |
|---|---|---|
| `Ess(0)/ActivePower` | 写入 | 设定的充放电功率 |
| `Ess(0)/Soc` | 读取 | 当前电池电量 |
| `Meter(0)/ActivePower` | 读取 | 并网点功率 |
| `Ess(0)/AllowedChargePower` | 读取 | 最大允许充电 |
| `Ess(0)/AllowedDischargePower` | 读取 | 最大允许放电 |
