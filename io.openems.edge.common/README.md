# io.openems.edge.common

> Edge 层公共 API 和基础组件，所有 Edge 模块都依赖它。

---

## 模块职责

这是 Edge 层最基础的模块，提供：
- 所有 Edge 组件的基类
- Channel（通道）机制
- 事件系统
- 公共类型定义

---

## 核心类

### 组件基类

| 类 | 说明 |
|---|---|
| `AbstractOpenemsComponent` | 所有 OpenEMS 组件的抽象基类 |
| `OpenemsComponent` | 组件公共接口 |

### Channel 机制

Channel 是 OpenEMS 的核心概念，是组件间数据交换的标准接口。

| 类 | 说明 |
|---|---|
| `Channel<T>` | 通道基类 |
| `ChannelId` | 通道 ID 枚举接口 |
| `Doc` | 通道元数据（类型、单位、描述） |
| `Value<T>` | 通道值包装类 |

### 事件系统

| 类 | 说明 |
|---|---|
| `EdgeEventConstants` | Edge 事件主题常量 |
| `EdgeComponent` | 组件事件接口 |

---

## 典型代码结构

```java
public class MyDevice extends AbstractOpenemsComponent implements MyNature {

    // 定义 ChannelId 枚举
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        GRID_CONNECTED(Doc.of(Level.INFO).text("并网状态"));

        private final Doc doc;
        private ChannelId(Doc doc) { this.doc = doc; }
        @Override public Doc doc() { return this.doc; }
    }

    public MyDevice() {
        super(OpenemsComponent.ChannelId.values(), MyDevice.ChannelId.values());
    }
}
```

---

## 事件周期

OpenEMS 是周期驱动的，每个 Cycle（默认 1 秒）：

```
TOPIC_CYCLE_BEFORE_PROCESS_IMAGE   ← 预处理阶段
  ↓
TOPIC_CYCLE_EXECUTE_WRITE         ← 写入阶段
  ↓
TOPIC_CYCLE_AFTER_PROCESS_IMAGE   ← 后处理阶段
```

---

## 依赖关系

```
所有 Edge 模块
  ↓ 依赖
io.openems.edge.common
  ↓ 依赖
io.openems.common
```

---

## 关键设计思想

1. **Channel 解耦**：组件之间不直接调用，通过 Channel 读写数据
2. **声明式 Channel**：ChannelId 是枚举，编译期类型安全
3. **周期驱动**：所有逻辑按 Cycle 执行，类似 PLC 的扫描周期
4. **OSGi 组件模型**：通过注解声明依赖、配置、生命周期
