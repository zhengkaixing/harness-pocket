# Harness Pocket Plugin Card SDK

`Harness Pocket` can render native cards for tools that return structured JSON.
The first supported card is the **result panel** (`plugin/card`). It is safe to
emit: the client never executes plugin code or loads remote assets.

## Tool name

```text
plugin/card
```

## Input

```json
{
  "title": "发布检查",
  "body": "3 个平台检查完成，2 个通过。",
  "items": [
    {"label": "B站", "value": "已发布 BV1FMW26oEfL"},
    {"label": "抖音", "value": "等待验证"},
    {"label": "微信", "value": "草稿已保存"}
  ],
  "actions": [
    {"label": "打开发布页", "url": "https://example.com/publish"}
  ]
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `title` | string | no | Card heading |
| `body` | string | no | Summary paragraph |
| `items` | array | no | Key/value rows, rendered in order |
| `actions` | array | no | Link buttons: `{"label":"...","url":"https://..."}` |

Actions open with the system browser via `ACTION_VIEW`; only `http(s)` links
should be used. Invalid or non-http URLs may be ignored by the platform.

If `title`, `body`, `items`, and `actions` are all empty, the tool falls back to the normal
tool card. Invalid JSON also falls back, so a broken plugin cannot break the
conversation.

## Result

The tool should return a normal tool result, for example:

```json
{"ok": true}
```

The card is rendered from the `tool/call` input, before the result arrives.

## Manifest (reserved)

```json
{
  "id": "example.publisher",
  "name": "Publisher Check",
  "version": "1.0.0",
  "tools": [
    {
      "name": "plugin/card",
      "card": "result",
      "description": "Show a publish status panel"
    }
  ]
}
```

`confirm` and `form` panels are reserved for a later protocol revision. They are
not executed by the current client; tools must not rely on them for approval or
input. Use DSH's normal approval/question events for those flows.
