# 事件 协议

> 协议号段：201-300
> 来源包：`com.slg.net.message.innermessage.event.packet`

## 协议列表

### FightSettleEvent

- **协议号**：201
- **类型**：内部事件
- **说明**：战斗结算事件
- **字段**：
  - `playerId` (long) — 玩家ID（带 @RoutePlayerGame 路由注解）
  - `win` (boolean) — 是否胜利
  - `killNum` (Map\<Integer, Long\>) — 击杀数量映射
