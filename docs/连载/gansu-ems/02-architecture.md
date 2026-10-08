---
title: ch02：拆开 OpenEMS——三层架构与 OSGi 模块解剖
slug: gansu-ems-02-architecture
---

决定改造 OpenEMS 的第二周，我犯了一个所有工程师都会犯的错误：打开 IDE，从第一个模块开始顺着读。读到第四天，我在 233 个模块里彻底迷路了——光 battery 相关就有 api、bmw、bydcommercial、fenecon.home、pylontech、soltaro 一长串，而我连"一个控制指令从产生到下发要经过哪几层"都说不清。

真正的转机来自换个问法：不问"这 60 万行代码是什么"，而问"一次功率指令的生命周期经过哪些对象"。顺着这条线，两天就读通了骨架。这一章，把我读通的骨架交给你。

## 一、事故现场：233 个模块从哪读起

先给规模一个直观感受。clone 下来统计：233 个 OSGi 模块、5315 个 Java 文件、约 60 万行 Java。模块命名是严格的 `io.openems.<层>.<领域>.<设备>` 格式，比如：

- `io.openems.edge.bridge.modbus` —— Edge 层的 Modbus 协议桥；
- `io.openems.edge.ess.huawei`（华为逆变器/储能驱动）、`io.openems.edge.battery.pylontech`（派能电池）；
- `io.openems.backend.timedata.iotdb` —— Backend 层的 Apache IoTDB 时序接入。

迷失的根源在于用"目录顺序"读代码，而模块目录是按设备名排的，不是按调用链排的。正确姿势是先看构建脚本拿到全景图：根目录 `settings.gradle.kts` 只有 60 行，核心逻辑是"扫描所有包含 `bnd.bnd` 且以 `io.openems` 开头的目录，自动注册为 Gradle 子项目"——也就是说，**一个目录有没有 `bnd.bnd`，就是它是不是一个可部署模块的唯一判据**。这一下就把 233 个模块的边界问题变成了文件系统问题。

## 二、梳理：三层架构，三层职责

OpenEMS 的顶层是三层，外加一个共享层：

<table>
<thead>
<tr><th>层</th><th>运行位置</th><th>职责</th><th>代表模块</th></tr>
</thead>
<tbody>
<tr><td>Edge</td><td>场站工控机</td><td>设备接入、实时控制、调度执行</td><td>edge.bridge.*、edge.ess.*、edge.controller.*</td></tr>
<tr><td>Backend</td><td>云端/数据中心</td><td>多场站汇聚、数据持久化、用户认证</td><td>backend.timedata.*、backend.metadata.*</td></tr>
<tr><td>UI</td><td>浏览器/手机</td><td>实时监控、组态展示</td><td>io.openems.ui（Angular）</td></tr>
<tr><td>Common</td><td>前三者共享</td><td>Channel 元模型、JSON-RPC 协议、工具类</td><td>io.openems.common</td></tr>
</tbody>
</table>

这个分层对我们的价值在 ch00 就说过：它跟 14 号令的安全分区要求天然同构——Edge 瘦身后进生产控制大区，Backend+UI 留在管理信息大区。但拆解之后我发现，同构不等于直接可用：OpenEMS 的 Edge 和 Backend 之间走 WebSocket 长连接（backend.b2bwebsocket 模块），这条链路要穿隔离装置，协议能不能透传是 ch10 的关键考题。

## 三、底层原理：OSGi 组件模型与 Nature/Channel 抽象

OpenEMS 全靠两个"零件"搭起来，理解了它们就读懂了 90% 的代码。

**第一个零件是 OSGi 组件模型。** 每个模块是一个 OSGi bundle，模块内的每个可配置单元（一台逆变器、一个控制器、一个协议桥）是一个 OSGi 声明式服务组件。组件之间不直接 new，而是通过 `@Reference` 注入依赖——比如功率分配控制器声明 `@Reference SymmetricEss[] esss`，运行时所有实现了 `SymmetricEss` 接口的组件都会被注入进来。配置由 OSGi ConfigAdmin 管理，每个组件实例对应一份 JSON 配置，这也是 OpenEMS 能在运行期增删设备、改配置不重启的底层原因。

**第二个零件是 Nature/Channel 抽象。** 这是整个项目最精妙的设计。`Nature` 是设备能力的 Java 接口，`Channel` 是设备身上带类型、带元数据的时序变量。看真实代码（我删减了注释）：

```java
@ProviderType
public interface SymmetricEss extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SOC(Doc.of(OpenemsType.INTEGER)//
                .unit(Unit.PERCENT)//
                .persistencePriority(PersistencePriority.HIGH)),
        ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
                .unit(Unit.WATT)//
                .persistencePriority(PersistencePriority.HIGH)),
        // ...
    }
}
```

读这段代码要知道三件事：第一，`SOC`、`ACTIVE_POWER` 这些不是字段，是 Channel 的枚举定义，每个 Channel 携带单位、类型、持久化优先级等元数据；第二，`ACTIVE_POWER` 的语义约定是"负值充电、正值放电"——全系统所有上层代码都依赖这个约定；第三，Nature 是接口不是类，华为逆变器和派能电池只要实现了 `SymmetricEss`，上层调度代码就一视同仁。

这套抽象直接回答了我们的建模问题：ch01 测点表里的每一行，落地就是某台设备上的一个 Channel；测点表的"设备无关性"诉求，落地就是 Nature 接口。

## 四、正确姿势：一次指令的完整生命周期

现在回答那个救了我两天阅读时间的问法。一次"储能放电 500kW"指令的生命周期：

1. **指令产生**：Backend 侧的调度引擎（或我们未来的 AGC 仲裁模块）产出指令，经 WebSocket 下发到 Edge；
2. **控制器执行**：Edge 的 Scheduler（`io.openems.edge.scheduler.api`）按固定周期轮询它管理的 Controller 列表，我们的功率分配逻辑就是一个 Controller 实现，在周期回调里被触发；
3. **Nature 抽象调用**：Controller 面向 `SymmetricEss` 接口编程，调用 `applyPower()` 语义的写入 Channel；
4. **驱动翻译**：具体的 Ess 实现（比如华为驱动）把 Channel 写入翻译成 Modbus 寄存器写操作，交给 `bridge.modbus` 的协议桥；
5. **链路下发**：协议桥按通道配置把报文发往设备，采样的回读数据沿原路反向流动，更新 Channel 的 Value——上层通过 `Value` 的 ChangeEvent 异步感知新值；
6. **聚合上报**：Edge 内置一个 `Sum` 组件（`io.openems.edge.common.sum.Sum`），自动把所有 Ess/Meter 的功率、电量聚合出 `_sum` 虚拟通道，UI 和 Backend 读的全是它。

读代码时拿任何一条真实指令沿这六步走一遍，走完你就有了 60 万行代码的"地图"。我用这六步走过的第一条指令，就是在仿真环境里让 FENECON 虚拟储能充了一次电。

## 五、数据说话：模块分布背后的工作量地图

把 233 个模块按层统计，画成改造工作量的"作战地图"：

- **Edge 层约 200 个模块**：设备驱动（ess/battery/pv/meter/chp/evcs 等）占大头，协议桥 6 个（modbus、iec104、mqtt、http、mbus、onewire），控制器 40+ 个。我们新写的 AGC 仲裁、功率分配控制器都挂在这一层；
- **Backend 层约 25 个模块**：时序数据（influx、iotdb 等 4 种实现）、元数据（odoo、file）、边缘通信（b2bwebsocket）；
- **Common 层 1 个模块**：被所有层依赖，改它最危险——我们给自己立的规矩是 Common 层原则上只加不改；
- **UI 层独立仓库目录**：Angular+Ionic，AGPL 许可，我们的策略是 UI 尽量不动、用组态能力拼大屏，避开 AGPL 传染。

这个分布直接支撑了 ch00 的复用率估算：我们动的几乎全是 Edge 层的"新增"而非"修改"，这也是开源改造风险最低的形态——上游升级时冲突面最小。

## 六、面试怎么答

"你们为什么选 OSGi？Spring 不行吗"——这题我在评审会上真被问过，答题要点：

1. **运行期热插拔是硬需求**：场站设备分批到货、检修换型号，EMS 必须运行期增删设备驱动实例而不停机，OSGi 的 bundle 生命周期和 ConfigAdmin 就是为此而生的；
2. **版本化的接口演进**：OSGi 的 `@ProviderType` + 语义化版本让设备驱动 API 的兼容性由框架强制，200 多个模块没有版本矩阵管理早就崩了；
3. **诚实承认代价**：bnd 工具链学习曲线陡、生态小众、招人难，我们的对策是把 OSGi 细节收敛在模块骨架层，业务代码按普通 Java 写；
4. **对比 Spring 留有余地**：如果控制实时性要求低、设备型号固定，Spring Boot 单体当然更简单——技术选型要看约束，不要看信仰。

## 七、落地清单

1. 读大型开源项目，先读构建脚本拿模块全景，再沿"一个指令的生命周期"读调用链，最后按需读实现细节；
2. OpenEMS 必读的五个入口：`settings.gradle.kts`（模块发现）、`io.openems.edge.ess.api` 的 `SymmetricEss.java`（Nature 样板）、`scheduler.api` 的 `Scheduler.java`（调度入口）、`edge.common` 的 `Sum.java`（聚合模型）、`edge.application`（装配清单）；
3. 牢记两个语义约定：`ACTIVE_POWER` 负充正放；Common 层只加不改；
4. 下一章（ch03），讲工程化落地：fork 策略、bnd/Gradle 工具链上手、EPL-2.0 合规清单怎么落到每个文件头，以及我们如何让 CI 在第一周就跑起来。
