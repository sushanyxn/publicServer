# 通知 协议

> 协议号段：1700-1799
> 来源包：`com.slg.net.message.clientmessage.notify.packet`

## 协议列表

### SM_NotifyMessage

- **协议号**：1700
- **类型**：服务端推送
- **说明**：服务端通知消息推送，服务端向客户端推送提示信息，客户端根据 infoId 查找 MessageTable 获取显示内容
- **字段**：
  - `infoId` (int) — 消息 ID，对应 MessageTable 中的 id
  - `msgType` (int) — 消息类型：1-普通消息，后续可扩展跑马灯等
