# io.openems.backend.ai

> **[新增]** Backend AI 扩展模块 — 告警 Agent + RAG 故障诊断，为后续大模型接入预留。

---

## 模块职责

在 Backend 层接入 AI 能力：
- **告警 Agent**：订阅系统告警事件，做告警风暴聚合、NER 实体提取、根因分析
- **RAG 故障诊断**：知识库文档切片、向量检索、故障诊断建议
- 通过 OSGi EventAdmin 事件总线与现有系统集成

---

## 核心类

| 类 | 说明 |
|---|---|
| `AlertAgent` | 告警处理 Agent |
| `RagDiagnosis` | RAG 故障诊断服务 |
| `AlertEvent` | 告警事件模型 |
| `DiagnosisResult` | 诊断结果模型 |
| `Config` | OSGi 配置类 |

---

## 告警 Agent 设计

### 告警风暴聚合

```
原始告警流 (每秒可能几百条)
        ↓
  ┌─────────────────┐
  │  时间窗口聚合    │  30 秒窗口
  │  同类告警合并    │  按设备/类型聚合
  │  级别提升       │  高频告警升级
  └────────┬────────┘
           ↓
  根因分析输入
```

### NER 实体提取

```
告警文本:
"储能变流器 PCS01 直流过压告警，电压 850V，额定 750V"

NER 提取:
  设备: PCS01
  部件: 直流侧
  异常: 过压
  当前值: 850V
  阈值: 750V
```

### 根因分析

```
告警 → 关联历史 → 规则匹配 → 输出根因假设
         ↓
  比如: 直流过压 → 关联到光伏MPPT异常 → 根因: 光伏板遮挡
```

---

## RAG 故障诊断设计

### 知识库构建

```
运维手册 PDF/Word
    ↓
  文档切片 (Chunk)
    ↓
  Embedding 向量化
    ↓
  向量数据库 (Milvus/Chroma)
```

### 检索流程

```
用户输入故障描述
    ↓
  Query Embedding
    ↓
  向量检索 Top-K 相关文档
    ↓
  LLM 生成诊断建议
    ↓
  返回结构化结果
```

---

## 事件集成

### 事件总线

```
OpenEMS 内部事件 (OSGi EventAdmin)
    ↓
TOPIC_ALERT_RAISED / TOPIC_ALERT_CLEARED
    ↓
AlertAgent 订阅
    ↓
处理 + 存储
```

### 无需 MQTT

- 直接用 OSGi EventAdmin，进程内事件
- 如果需要外部系统对接，再用 MQTT 桥接

---

## 配置参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `alertWindowSize` | 30s | 告警聚合窗口 |
| `maxAlertsPerWindow` | 100 | 窗口内最大告警数 |
| `llmApiUrl` | — | LLM API 地址 |
| `llmApiKey` | — | LLM API Key |
| `vectorDbUrl` | — | 向量数据库地址 |
| `embeddingModel` | bge-large | Embedding 模型 |

---

## 未来扩展路线

### Phase 1 (当前)
- [x] 模块骨架
- [x] 事件订阅框架
- [x] 数据模型定义

### Phase 2
- [ ] 告警聚合算法实现
- [ ] 接入 LLM API
- [ ] 文档切片工具

### Phase 3
- [ ] 向量数据库接入
- [ ] 故障诊断 Prompt 优化
- [ ] 诊断结果回写业务库

---

## 与现有系统的关系

```
Backend
├── alerting         原生告警服务
│     ↓ 事件
├── ────────────────┘
│
└── ai              新增 AI 扩展
      ├── AlertAgent   告警分析
      └── RagDiagnosis 故障诊断
```

**设计原则**：AI 模块不侵入现有系统，只通过事件订阅方式接入，随时可以开关。
