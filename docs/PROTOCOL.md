# DSH Remote API 笔记

这份文档记录 Harness Pocket 用到的 DeepSeek Harness（DSH）Remote API 形状。它不是官方规范，而是从桌面端 `app.asar` 中的 `dsh-client-connection`、`dsh-api-gateway`、`dsh-api-session-controller` 源码和实测响应里整理出来的。DSH 升级后字段可能变化，遇到问题请先对照服务端版本。

## 1. 认证：用启动令牌换 Cookie

`dsh web` 每次启动都会生成一个随机 launch token，并打印：

```text
http://host:port/?token=<token>
```

浏览器打开这个 URL 后会换到签名 Cookie。原生客户端走同一条 `/connect` 通道：

```http
GET /connect?token=<url-encoded-token>&interactions=none HTTP/1.1
Host: host:port
```

成功时：

```http
HTTP/1.1 302 Found
Location: ./api/remote.mux
Set-Cookie: dsh-auth-<authority-hash>=v2...; Max-Age=2592000; Path=/; HttpOnly; SameSite=Strict
```

要点：

- 必须把 `token` 放在 query 中，不能用 `Authorization` 头。
- `interactions=none` 表示客户端不接收/回答审批交互；`interactions=answer` 才会收到审批事件。
- 同一次请求里 `interactions` 只能声明一次；重复 query 参数会返回 400。
- Cookie 名称以 `dsh-auth-` 开头，绑定 Host（authority）；换域名或端口后需要重新 `/connect`。
- 后续 HTTP 和 WebSocket 都带上这个 Cookie。

## 2. HTTP RPC

所有普通方法都是：

```http
POST /api/{namespace}/{method}
Content-Type: application/json
Cookie: dsh-auth-...=...
```

请求体：

```json
{
  "type": "client-request",
  "rpcId": "2f0c...uuid",
  "method": "session/list",
  "payload": {
    "args": {}
  }
}
```

响应体：

```json
{
  "type": "server-response",
  "rpcId": "2f0c...uuid",
  "result": {
    "ok": true,
    "value": {}
  }
}
```

失败时：

```json
{
  "type": "server-response",
  "rpcId": "2f0c...uuid",
  "result": {
    "ok": false,
    "error": {
      "code": "gateway/arguments-invalid",
      "message": "missing \"_request\"",
      "details": {}
    }
  }
}
```

客户端应校验响应中的 `rpcId` 与请求一致；HTTP 非 2xx 视为传输层失败。`401` 通常意味着 Cookie 过期，可以用保存的 launch token 重新 `/connect` 后重试一次。

## 3. 已使用的方法

### session/list

```json
{ "args": { "_request": {} } }
```

返回：

```json
{
  "value": {
    "items": [
      {
        "sessionId": "session-...",
        "updatedAt": 1760000000000,
        "running": false,
        "blank": false,
        "cwd": "C:\\work\\project",
        "agentAvailable": true,
        "projections": {
          "asOfSeq": 42,
          "values": { "title": "修复登录页" }
        }
      }
    ]
  }
}
```

注意：参数名是 `_request`，传 `{}` 会报 `gateway/arguments-invalid ... missing "_request"`。

`projections.asOfSeq` 是该会话当前最新事件序号，也是 `session/page` 的 `throughSeq` 上界。

### session/page

```json
{
  "args": {
    "request": {
      "address": { "kind": "session", "sessionId": "session-..." },
      "throughSeq": 42,
      "maxMessages": 80,
      "turnWindow": { "minMessages": 24, "minTurns": 4 }
    }
  }
}
```

返回：

```json
{
  "value": {
    "records": [
      { "type": "event", "event": { "type": "user/message", "seq": 8, "time": 1760000000000, "data": {} } }
    ],
    "hasMore": false
  }
}
```

`throughSeq` 不能超过服务端当前 cursor，否则会返回类似：

```text
session page through seq 999999 is past cursor 18
```

没有先开 `session/follow` 时，用 `session/list` 行里的 `projections.asOfSeq` 最稳。

### session/create

```json
{ "args": { "request": {} } }
```

返回 `{ "sessionId": "session-...", "agentPreset": "standard" }`。可以传 `cwd`、`workspaceId`、`agentPreset`、`sessionId`。

### session/prompt

```json
{
  "args": {
    "request": {
      "requestId": "uuid",
      "sessionId": "session-...",
      "mode": "queue",
      "content": [{ "type": "text", "text": "继续" }],
      "clientTimeZone": "Asia/Shanghai"
    }
  }
}
```

返回 `{ "accepted": true }`。`requestId` 是幂等键，网络重试时复用同一个值。

`content` 是判别联合，已知类型：

- `{"type":"text","text":"..."}`
- `{"type":"image","mediaType":"image/png","data":"<base64>","name":"photo.png"}`
- `{"type":"file","receiptId":"..."}`

`image.mediaType` 只接受 `image/png`、`image/jpeg`、`image/webp`、`image/gif`。图片可以直接内联；任意文件需要先调用 `fileUploads/upload` 换取 `receiptId`。至少要有一个非空白 `text` 或附件。

### userQuestions

`session/list` 的 row 投影 `projections.values.userQuestions`：

```json
{
  "active": [{
    "callId": "call_...",
    "state": "open | continued",
    "questions": [{
      "id": "fruit",
      "header": "需要你确认",
      "question": "午餐想吃哪种水果？",
      "detail": "可选填",
      "multiSelect": true,
      "options": [{ "label": "苹果", "description": "清脆" }]
    }]
  }],
  "settled": [{ "callId": "call_...", "answers": [{ "id": "fruit", "selected": ["苹果"] }] }]
}
```

只有 `state == "continued"` 能通过 `userQuestions/answer` 回答；`open` 状态需要 live waterfall 事件通道。答案必须覆盖 call 内每个 question 恰好一次，否则报 `BAD_ANSWER`；已有排队回复时报 `REPLY_QUEUED`。

### 其他

| 方法 | args | 说明 |
| --- | --- | --- |
| `session/cancel` | `{"request":{"sessionId":"..."}}` | 停止当前 turn |
| `session/rename` | `{"request":{"sessionId":"...","title":"..."}}` | 重命名 |
| `session/selectModel` | `{"request":{"sessionId":"...","provider":"...","model":"...","reasoningEffort":"low"}}` | 切换模型 |
| `session/modelCatalog` | `{}` | 模型目录 |
| `permissionPresets/catalog` | `{}` | 权限预设目录 |
| `commands/execute` | `{"agentId":"...","line":"/permission read-only","submittedAttachments":[]}` | 执行斜杠命令（权限切换也走这里） |
| `fileUploads/upload` | `{"agentId":"...","request":{"data":"<base64>","name":"..."}}` | 上传文件换 receiptId |
| `userQuestions/answer` | `{"agentId":"...","callId":"...","answer":{"answers":[{"id":"...","selected":["..."],"custom":"..."}]}}` | 回答 `userQuestions` 投影中的 continued 问题；返回 boolean |
| `workspace/create` | `{"request":{"path":"C:\\work"}}` | 创建工作区 |
| `workspace/insertBefore` | `{"request":{"workspaceId":"...","beforeWorkspaceId":"..."}}` | 调整文件夹顺序；省略 anchor 追加到末尾 |
| `workspace/pinSession` | `{"request":{"sessionId":"..."}}` | 置顶会话 |
| `workspace/unpinSession` | `{"request":{"sessionId":"..."}}` | 取消置顶 |
| `workspace/archiveSession` | `{"request":{"sessionId":"...","stopActivity":true}}` | 归档；运行中可先 stop |
| `workspace/unarchiveSession` | `{"request":{"sessionId":"..."}}` | 恢复归档 |
| `skills/list` | `{"_request":{}}` | 技能列表（未在 App 中使用） |
| `fileReferences/list` | 视 schema | 文件引用（未使用） |

### session/modelCatalog

无参数，返回：

```json
{
  "default": { "provider": "opencode-go", "model": "deepseek-v4.1-flash", "reasoningEffort": "max" },
  "routableProviders": ["opencode-go"],
  "groups": [
    {
      "id": "opencode-go",
      "name": "opencode-go",
      "models": [
        {
          "id": "deepseek-v4.1-flash",
          "name": "DeepSeek V4.1 Flash",
          "reasoning": {
            "efforts": [{ "id": "max", "name": "Max" }],
            "defaultEffort": "max"
          }
        }
      ]
    }
  ],
  "failures": []
}
```

`group.id` 是 provider，`models[].id` 是 model，`reasoning.efforts[].id` 是 `reasoningEffort`。部分模型的 `defaultEffort` 为空，客户端需要让用户显式选择；否则服务端可能落到 `off`。

### permissionPresets/catalog

无参数，返回：

```json
{
  "options": [
    { "value": "read-only", "name": "仅可查看" },
    { "value": "workspace-write", "name": "工作区内修改" },
    { "value": "danger-full-access", "name": "完全权限" }
  ],
  "defaultPreset": "workspace-write"
}
```

权限切换没有独立 mutation RPC，必须执行命令：

```json
{
  "args": {
    "agentId": "session-...",
    "line": "/permission read-only",
    "submittedAttachments": []
  }
}
```

`agentId` 就是 sessionId。返回：

```json
{
  "commandId": "cmd-...",
  "result": { "kind": "success", "text": "preset read-only" }
}
```

`/permission`（不带参数）可以查询当前值与可选项。

### fileUploads/upload

```json
{
  "args": {
    "agentId": "session-...",
    "request": { "data": "<base64>", "name": "notes.txt" }
  }
}
```

返回：

```json
{
  "receiptId": "receipt-...",
  "file": { "attachmentId": "...", "name": "notes.txt", "bytes": 1234 }
}
```

把 `receiptId` 放进 `session/prompt` 的 `{"type":"file","receiptId":"..."}`。

### session/attachment

事件里的 image block 只带 `attachment` 元数据，不含图片字节。读取时传 attachmentId：

```json
{
  "args": {
    "request": {
      "sessionId": "session-...",
      "attachmentId": "..."
    }
  }
}
```

返回 `attachment` 元数据和 base64 `data`，客户端解码后用 `BitmapFactory` 显示。

### 资源与并发（dsh-conversation-queue 插件）

插件在 dsh web 同源暴露两个路由，使用登录 Cookie 直接请求：

```text
GET  /dsh-resource/status
POST /dsh-resource/limit   {"maxConcurrent": 1..8}
```

`status` 返回内存、提交内存、磁盘和并发闸门；未安装插件时路由为 404。

### 工作区

工作区列表只能通过 WebSocket 的 `workspace/follow` 流获取。`open` 帧：

```json
{
  "type": "open",
  "streamId": "ws1",
  "endpoint": "workspace/follow",
  "payload": { "args": {} }
}
```

首帧：

```json
{
  "type": "baseline",
  "value": {
    "items": [
      {
        "workspaceId": "ws-...",
        "path": "C:\\work\\project",
        "title": "project",
        "sessionIds": ["session-..."],
        "createdAt": 1760000000000,
        "updatedAt": 1760000000000
      }
    ],
    "archivedSessionIds": [],
    "pinnedSessionIds": []
  }
}
```

后续还有 upsert / remove / order / archived / pinned 帧。新建会话时传 `workspaceId` 或 `cwd`，二者互斥。

## 4. WebSocket mux

工作区列表已经通过这里实现；会话消息仍用 `session/page` 轮询。

建立连接：

```text
ws(s)://{host}/api/remote.mux
Cookie: dsh-auth-...
```

客户端帧：

```json
{"type":"open","streamId":"s1","endpoint":"session/follow","payload":{"args":{"request":{"address":{"kind":"session","sessionId":"session-..."},"assistantStream":true}}}}
{"type":"item","streamId":"s1","value":{}}
{"type":"end","streamId":"s1"}
{"type":"cancel","streamId":"s1"}
```

服务端帧只允许三种形状：

```json
{"type":"item","streamId":"s1","value":{}}
{"type":"end","streamId":"s1"}
{"type":"error","streamId":"s1","error":{"code":"...","message":"...","details":{}}}
```

`session/follow` 的首帧是快照：

```json
{
  "type": "snapshot",
  "header": {},
  "cursor": 42,
  "records": [{ "type": "event", "event": {} }],
  "hasMore": false,
  "projections": {}
}
```

之后是增量事件帧。`assistantStream: true` 时还会收到无 cursor 的 assistant 增量帧，用于逐字渲染。当前 App 用 `session/page` 轮询，后续计划切换到这里。

## 5. 事件模型

每条持久事件：

```json
{
  "type": "assistant/message",
  "seq": 29,
  "time": 1760000000000,
  "data": {},
  "ignorable": false,
  "surfaceOp": "append",
  "sourceEventSeqs": [27, 28]
}
```

App 目前处理的事件：

### user/message

```json
{
  "message": {
    "role": "user",
    "source": { "kind": "user", "rpcId": "..." },
    "content": [{ "type": "text", "text": "你好" }],
    "id": "..."
  }
}
```

只展示 `source.kind == "user"` 的消息。运行时注入的 `runtime-context`、`skill-catalog`、`time-context` 等也会以 user/message 出现，需要跳过。

### assistant/message

```json
{
  "turn": 3,
  "step": 1,
  "message": {
    "role": "assistant",
    "source": { "kind": "model", "provider": "deepseek-account", "model": "deepseek-flash" },
    "content": [
      { "type": "reasoning", "text": "" },
      { "type": "tool-call", "id": "call_...", "name": "pwsh", "arguments": "{\"command\":\"echo hi\"}" }
    ],
    "id": "..."
  },
  "usage": {},
  "stream": []
}
```

`content` 可能同时包含 `reasoning`、`text`、`tool-call`。App 把 `text` 拼成助手气泡，把 `tool-call` 建成工具卡；`reasoning` 暂时不展示。

### tool/call

```json
{
  "turn": 3,
  "step": 1,
  "callId": "call_...",
  "name": "pwsh",
  "arguments": "{\"description\":\"Echo\",\"command\":\"echo hi\"}"
}
```

`arguments` 是 JSON 字符串，不是对象。

### tool/result

```json
{
  "turn": 3,
  "step": 1,
  "message": {
    "role": "tool",
    "source": { "kind": "tool", "callId": "call_..." },
    "toolCallId": "call_...",
    "isError": false,
    "content": [{ "type": "text", "text": "hi\r\n" }],
    "id": "..."
  },
  "meta": { "cwd": "C:\\work" }
}
```

用 `message.source.callId`（退而求其次 `message.toolCallId`）把结果回填到对应工具卡。

### turn/end / step/end

```json
{ "reason": { "kind": "error", "error": { "code": "...", "message": "..." } } }
```

`turn/end` 的错误会变成红色 Notice。`step/end` 也可能带 `error.message`。

### 其他常见类型

- `approval/policy`：`{"policy":"ask"}`。
- `sandbox/mode`：`{"mode":"workspace-write"}`。
- `approval/asked` / `approval/decided`：审批请求与结果。
- `turn/start`、`step/start`、`session/title`。
- `request/header`、`request/context`：可能非常大，渲染时跳过。

## 6. 实测注意点

1. `session/list` 必须传 `_request`，不能传空对象。
2. `session/page` 的 `throughSeq` 不能超过 `projections.asOfSeq`。
3. `tool/call` 与 `assistant/message` 里的 `tool-call` 可能表达同一件事；App 以 `assistant/message` 的 `tool-call` 建卡，以 `tool/result` 回填，避免重复。
4. `request/header` 这类事件可能比正常消息大几十倍，分页时不要盲目放大 `maxMessages`。
5. 服务端的 `approval/policy` 可能是 `ask`；沙箱内自动放行的命令仍可直接执行，需要人工审批的操作在 `interactions=none` 下不会被 App 处理。

## 7. 免责声明

本项目与 DeepSeek 官方无关。协议来自对本地安装包和自有服务端的观察，仅用于个人远程访问。请只连接你自己拥有或获授权的 Harness 实例。
