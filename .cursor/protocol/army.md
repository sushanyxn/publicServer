# 军团/部队 协议

> 协议号段：1300-1399
> 来源包：`com.slg.net.message.clientmessage.army.packet`

## 协议列表

### ArmyVO

- **协议号**：1300
- **类型**：数据体(VO)
- **说明**：军队 VO（抽象基类）
- **字段**：
  - `id` (long) — 军队实体 id（场景或全局唯一，视子类用途而定）

### AssembleArmyVO

- **协议号**：1301
- **类型**：数据体(VO)
- **说明**：集结军队 VO
- **继承**：ArmyVO
- **字段**：
  - `leaderId` (long) — 集结队长玩家 id
  - `members` (List\<PlayerArmyVO\>) — 集结成员军队列表

### PlayerArmyVO

- **协议号**：1302
- **类型**：数据体(VO)
- **说明**：玩家军队 VO
- **继承**：ArmyVO
- **字段**：
  - `heroes` (HeroVO[]) — 军队内英雄列表
  - `troops` (TroopVO[]) — 军队内兵种列表（所有英雄共同带领一队兵）

### TroopVO

- **协议号**：1303
- **类型**：数据体(VO)
- **说明**：部队/士兵 VO
- **字段**：
  - `troopId` (int) — 兵种配置 id
  - `num` (int) — 当前数量（剩余）
  - `initNum` (int) — 初始/出征数量
  - `hurtNum` (int) — 轻伤数量

### WorldBossArmyVO

- **协议号**：1304
- **类型**：数据体(VO)
- **说明**：世界 Boss 军队 VO
- **继承**：ArmyVO
- **字段**：
  - `configId` (int) — 世界 Boss 配置 id
