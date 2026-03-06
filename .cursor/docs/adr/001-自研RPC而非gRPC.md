# ADR-001: 自研 RPC 而非 gRPC

## 状态

已采纳

## 背景

项目需要 Game↔Scene 进程间 RPC 调用能力。客户端与服务端已使用 Netty WebSocket + 自定义二进制协议（message.yml 注册、MessageCodec 编解码），所有协议类（CM_/SM_/IM_/VO/Event）共用同一套注册与编解码体系。

可选方案包括：
- 自研 RPC（复用现有 Netty + message.yml 体系）
- gRPC（HTTP/2 + Protobuf，标准多语言）
- Dubbo / RSocket

## 决策

采用**自研 RPC**，基于现有 Netty WebSocket + message.yml 协议体系，通过 `IM_RpcRequest` / `IM_RpcRespone` 内部消息承载 RPC 请求/响应。支持直连（ServerIdRoute，走 WebSocket）与 Redis Stream 路由（RedisRoute，跨服转发）。

## 理由

1. **与现有协议统一**：RPC 请求/响应复用 message.yml 注册与 MessageCodec 编解码，无需引入 .proto 与代码生成
2. **客户端与内部共用一套**：客户端协议（CM_/SM_）与内部 RPC（IM_）走同一个 Netty 管道，降低维护成本
3. **路由灵活**：`@RpcMethod(routeClz = ...)` 声明路由策略，直连与 Redis Stream 路由可按需切换
4. **轻量无额外依赖**：无需 gRPC 运行时、Protobuf 编译器或 Dubbo Broker

## 后果

- 不具备原生多语言支持；若未来需要非 JVM 服务调用，需为其实现相同编解码或为内部 RPC 层试点 gRPC
- RPC 接口定义（`@RpcMethod`）、路由、线程分派均为项目自有约定，新成员需学习
- Redis Stream 路由的吞吐受单 Redis 实例限制（约 1.5k-2k ops/s），极大规模时需评估 Kafka 等替代
