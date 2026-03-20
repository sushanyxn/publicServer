# GM 协议

> 协议号段：1600-1699
> 来源包：`com.slg.net.message.clientmessage.gm.packet`

## 协议列表

### CM_GMCommand

- **协议号**：1600
- **类型**：客户端请求
- **说明**：GM 指令请求，客户端发送的 GM 指令字符串，格式为 "方法名 参数1 参数2 ..."
- **字段**：
  - `command` (String) — GM 指令字符串，如 "gainHero 1001"

### SM_GMResult

- **协议号**：1601
- **类型**：服务端推送
- **说明**：GM 指令执行结果，服务端返回给客户端的 GM 执行结果
- **字段**：
  - `command` (String) — 原始指令
  - `code` (int) — 错误码：0 成功，1 失败
  - `message` (String) — 结果描述
