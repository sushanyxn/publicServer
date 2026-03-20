# 任务 协议

> 协议号段：1200-1299
> 来源包：`com.slg.net.message.clientmessage.task.packet`

## 协议列表

### CM_GainTask

- **协议号**：1200
- **类型**：客户端请求
- **说明**：领取任务奖励请求，客户端完成任务后请求领取奖励
- **字段**：
  - `type` (int) — 任务类型
  - `id` (int) — 任务ID

### SM_MainTaskInfo

- **协议号**：1201
- **类型**：服务端推送
- **说明**：主线任务信息推送，服务端推送给客户端的主线任务列表
- **字段**：
  - `tasks` (List\<TaskVO\>) — 主线任务列表

### SM_UpdateTask

- **协议号**：1202
- **类型**：服务端推送
- **说明**：任务更新推送，服务端推送给客户端的任务进度更新消息
- **字段**：
  - `task` (TaskVO) — 更新的任务信息

### TaskVO

- **协议号**：1203
- **类型**：数据体(VO)
- **说明**：任务值对象，客户端显示任务信息的数据传输对象
- **字段**：
  - `id` (long) — 任务ID
  - `type` (int) — 任务类型
  - `progress` (long) — 当前进度
  - `isFinished` (boolean) — 是否已完成
  - `isGained` (boolean) — 是否已领取奖励
