# RPC 协议

> 协议号段：101-110
> 来源包：`com.slg.net.message.innermessage.rpc.packet`

## 协议列表

### IM_RpcRequest

- **协议号**：101
- **类型**：内部消息
- **说明**：RPC 请求消息，支持 Deadline 机制（借鉴 gRPC）
- **字段**：
  - `sourceServerId` (int) — 来源服务器ID
  - `methodMarker` (String) — RPC 方法标识：接口全限定名 + "#" + 方法名
  - `params` (Object[]) — RPC 方法传参
  - `callBackId` (long) — 请求ID（用于响应匹配，0 表示无需响应）
  - `deadlineMillis` (long) — 截止时间（绝对时间戳，毫秒）；0 表示无超时限制

### IM_RpcRespone

- **协议号**：102
- **类型**：内部消息
- **说明**：RPC 响应消息
- **字段**：
  - `id` (long) — 请求ID（用于响应匹配）
  - `result` (Object) — 响应结果
  - `error` (String) — 异常信息（如果有）
