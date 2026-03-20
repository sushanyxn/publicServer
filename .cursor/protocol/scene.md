# 场景 协议

> 协议号段：1100-1199
> 来源包：`com.slg.net.message.clientmessage.scene.packet`

## 协议列表

### CM_EnterScene

- **协议号**：1100
- **类型**：客户端请求
- **说明**：请求进入场景
- **字段**：
  - `serverId` (int) — 场景服 serverId
  - `sceneId` (int) — 场景 id（如主城、世界地图）

### CM_LoadSceneFinish

- **协议号**：1101
- **类型**：客户端请求
- **说明**：客户端加载地图完成，正式进入场景
- **字段**：无

### CM_Watch

- **协议号**：1102
- **类型**：客户端请求
- **说明**：场景查看协议
- **字段**：
  - `x` (int) — 移动后的 x 坐标
  - `y` (int) — 移动后的 y 坐标
  - `layer` (int) — 层级

### SM_EnterScene

- **协议号**：1103
- **类型**：服务端推送
- **说明**：请求进入场景的返回
- **字段**：
  - `result` (int) — 0 = 校验通过，可以切图；其他 = 错误码

### SM_SceneNodeAppear

- **协议号**：1104
- **类型**：服务端推送
- **说明**：场景节点出现消息
- **字段**：
  - `node` (SceneNodeVO) — 出现的场景节点

### SM_SceneNodeDisappear

- **协议号**：1105
- **类型**：服务端推送
- **说明**：场景节点消失消息
- **字段**：
  - `id` (long) — 消失的节点实体 id（场景内唯一）

### AllianceOwnerVO

- **协议号**：1106
- **类型**：数据体(VO)
- **说明**：联盟所有者 VO
- **继承**：OwnerVO
- **字段**：
  - `allianceId` (long) — 联盟 id
  - `allianceShortName` (String) — 联盟简称
  - `allianceName` (String) — 联盟名称
  - `allianceFlag` (String) — 联盟旗帜

### FPositionVO

- **协议号**：1107
- **类型**：数据体(VO)
- **说明**：亚格子位置 VO（定点数，放大 100 倍），使用 int 存储，实际坐标 = 值 / 100
- **字段**：
  - `x` (int) — X 坐标（放大 100 倍）
  - `y` (int) — Y 坐标（放大 100 倍）

### MonsterOwnerVO

- **协议号**：1108
- **类型**：数据体(VO)
- **说明**：怪物所有者 VO
- **继承**：OwnerVO
- **字段**：
  - `configId` (int) — 怪物配置 id

### OwnerVO

- **协议号**：1109
- **类型**：数据体(VO)
- **说明**：所有者 VO（抽象基类）
- **字段**：无

### PlayerCityVO

- **协议号**：1110
- **类型**：数据体(VO)
- **说明**：玩家城市 VO
- **继承**：StaticNodeVO → SceneNodeVO
- **字段**：无（继承自 StaticNodeVO）

### PlayerOwnerVO

- **协议号**：1111
- **类型**：数据体(VO)
- **说明**：玩家所有者 VO
- **继承**：OwnerVO
- **字段**：
  - `playerId` (long) — 玩家 id
  - `allianceId` (long) — 联盟 id（0 表示无联盟）
  - `headerId` (int) — 头像配置 id
  - `cityPosition` (PositionVO) — 主城/据点位置

### PositionVO

- **协议号**：1112
- **类型**：数据体(VO)
- **说明**：位置 VO
- **字段**：
  - `x` (int) — 格子 x 坐标
  - `y` (int) — 格子 y 坐标

### ResourceNodeVO

- **协议号**：1113
- **类型**：数据体(VO)
- **说明**：资源节点 VO
- **继承**：StaticNodeVO → SceneNodeVO
- **字段**：无（继承自 StaticNodeVO）

### RouteNodeVO

- **协议号**：1114
- **类型**：数据体(VO)
- **说明**：路径节点 VO
- **继承**：SceneNodeVO
- **字段**：
  - `startPosition` (FPositionVO) — 路径起点（亚格子坐标）
  - `endPosition` (FPositionVO) — 路径终点（亚格子坐标）
  - `armyVO` (ArmyVO) — 该路径节点上的军队信息

### SceneNodeVO

- **协议号**：1115
- **类型**：数据体(VO)
- **说明**：场景节点 VO（抽象基类）
- **字段**：
  - `id` (long) — 节点实体 id（场景内唯一）
  - `owner` (OwnerVO) — 节点归属（玩家/联盟/怪物等）

### ScenePlayerVO

- **协议号**：1116
- **类型**：数据体(VO)
- **说明**：需要透传到 scene 的 player 信息
- **字段**：
  - `playerId` (long) — 玩家 id
  - `gameServerId` (int) — 玩家所在游戏服 serverId
  - `allianceId` (long) — 玩家联盟

### StaticNodeVO

- **协议号**：1117
- **类型**：数据体(VO)
- **说明**：静态节点 VO（抽象基类）
- **继承**：SceneNodeVO
- **字段**：
  - `position` (PositionVO) — 节点所在地图格子位置

### SM_SceneArmyAppear

- **协议号**：1118
- **类型**：服务端推送
- **说明**：场景内军队出现推送，服务端在场景中某支军队对客户端可见时下发
- **字段**：
  - `id` (long) — 军队实体 id（场景内唯一）

### SM_SceneArmyDisappear

- **协议号**：1119
- **类型**：服务端推送
- **说明**：场景内军队消失推送，服务端在场景中某支军队对客户端不再可见时下发
- **字段**：
  - `id` (long) — 军队实体 id（场景内唯一）
