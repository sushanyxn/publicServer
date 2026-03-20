# 战报 协议

> 协议号段：1500-1599
> 来源包：`com.slg.net.message.clientmessage.report.packet`

## 协议列表

### ReportVO

- **协议号**：1500
- **类型**：数据体(VO)
- **说明**：战报/报告顶层 VO，按模块类型（key）聚合各子模块数据，用于客户端展示战斗报告、采集报告等
- **字段**：
  - `reportModules` (Map\<Integer, ReportModuleVO\>) — 战报子模块映射，key 为模块类型标识，value 为对应模块 VO

### ReportModuleVO

- **协议号**：1501
- **类型**：数据体(VO)
- **说明**：战报子模块抽象基类，各类战报模块（基础信息、英雄、兵种、属性、科技、录像等）继承本类，由 ReportVO 按类型聚合
- **字段**：无

### ReportBaseModuleVO

- **协议号**：1502
- **类型**：数据体(VO)
- **说明**：战报基础模块 VO，描述进攻方、防守方、战斗胜负及战斗地点等基础信息
- **继承**：ReportModuleVO
- **字段**：
  - `attacker` (OwnerVO) — 进攻方信息
  - `defender` (OwnerVO) — 防守方信息
  - `attackerWin` (boolean) — 是否进攻方胜利
  - `position` (PositionVO) — 战斗发生地点

### AttributeModuleVO

- **协议号**：1503
- **类型**：数据体(VO)
- **说明**：战报属性模块 VO，描述进攻方与防守方在战斗中的属性数据
- **继承**：ReportModuleVO
- **字段**：
  - `attackerAttribute` (FightAttributeVO) — 进攻方战斗属性
  - `defenderAttribute` (FightAttributeVO) — 防守方战斗属性

### HeroModuleVO

- **协议号**：1504
- **类型**：数据体(VO)
- **说明**：战报英雄模块 VO，描述进攻方与防守方参战英雄的简要信息
- **继承**：ReportModuleVO
- **字段**：
  - `attackerHeroes` (FightHeroVO[]) — 进攻方英雄列表
  - `defenderHeroes` (FightHeroVO[]) — 防守方英雄列表

### TroopModuleVO

- **协议号**：1505
- **类型**：数据体(VO)
- **说明**：战报兵种模块 VO，描述进攻方与防守方参战兵种的数量与伤亡
- **继承**：ReportModuleVO
- **字段**：
  - `attackerTroops` (FightTroopVO[]) — 进攻方兵种列表
  - `defenderTroops` (FightTroopVO[]) — 防守方兵种列表

### TechnologyModuleVO

- **协议号**：1506
- **类型**：数据体(VO)
- **说明**：战报科技模块，用于战报中科技相关数据的承载
- **继承**：ReportModuleVO
- **字段**：
  - `attacker` (FightTechnologyVO) — 进攻方科技数据
  - `defender` (FightTechnologyVO) — 防守方科技数据

### FightTechnologyVO

- **协议号**：1507
- **类型**：数据体(VO)
- **说明**：战报科技数据 VO，描述战斗中生效的科技研究进度
- **字段**：
  - `techRate` (Map\<Integer, Integer\>) — 科技 id → 研究百分比（0-100）

### MemberModuleVO

- **协议号**：1508
- **类型**：数据体(VO)
- **说明**：战报成员模块 VO，用于集结战等场景，描述进攻方与防守方各成员信息
- **继承**：ReportModuleVO
- **字段**：
  - `attackerMembers` (FightMemberVO[]) — 进攻方成员列表
  - `defenderMembers` (FightMemberVO[]) — 防守方成员列表

### VideoModuleVO

- **协议号**：1509
- **类型**：数据体(VO)
- **说明**：战报录像模块 VO，描述本场战报关联的录像 id，用于客户端拉取或播放战斗回放
- **继承**：ReportModuleVO
- **字段**：
  - `videoId` (long) — 录像 id，用于拉取/播放回放

### FightAttributeVO

- **协议号**：1510
- **类型**：数据体(VO)
- **说明**：战斗属性 VO，描述一方在战斗中的属性：常规展示属性与额外加成属性
- **字段**：
  - `showAttributes` (Map\<Integer, Integer\>) — 常规展示属性，属性类型 id → 数值
  - `extraAttributes` (Map\<Integer, Integer\>) — 额外加成属性，属性类型 id → 数值

### FightHeroVO

- **协议号**：1511
- **类型**：数据体(VO)
- **说明**：战斗英雄简要 VO，描述单名参战英雄的配置 id 与等级
- **字段**：
  - `heroId` (int) — 英雄配置 id
  - `heroLv` (int) — 英雄等级

### FightTroopVO

- **协议号**：1512
- **类型**：数据体(VO)
- **说明**：战斗兵种详情 VO，描述单类兵种在战斗中的数量与伤亡
- **字段**：
  - `troopId` (int) — 兵种配置 id
  - `initNum` (int) — 战斗开始时兵力
  - `hurtNum` (int) — 轻伤数量
  - `seriousNum` (int) — 重伤数量
  - `deadNum` (int) — 死亡数量
  - `leastNum` (long) — 剩余数量

### FightMemberVO

- **协议号**：1513
- **类型**：数据体(VO)
- **说明**：战斗成员 VO，描述集结等场景下单个成员的拥有者信息及其兵种战斗数据
- **字段**：
  - `owner` (OwnerVO) — 成员拥有者信息
  - `fightTroop` (FightTroopVO) — 该成员的兵种战斗数据
