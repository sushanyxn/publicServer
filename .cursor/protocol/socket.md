# SOCKET 协议

> 协议号段：111-120
> 来源包：`com.slg.net.message.innermessage.socket.packet`

## 协议列表

### IM_RegisterSessionRequest

- **协议号**：111
- **类型**：内部消息
- **说明**：内部链接注册消息
- **字段**：
  - `sourceServerId` (int) — 发起注册的源服 serverId

### IM_RegisterSessionResponce

- **协议号**：112
- **类型**：内部消息
- **说明**：内部链接注册返回
- **字段**：
  - `result` (int) — 注册结果（0 成功，非 0 失败）
  - `needReInit` (boolean) — 是否需要重新初始化（首次连接或宽限期已过为 true，宽限期内重连为 false）
