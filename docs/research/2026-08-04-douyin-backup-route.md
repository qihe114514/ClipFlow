# Douyin backup route research

调查日期：2026-08-04

上游仓库：`Evil0ctal/Douyin_TikTok_Download_API`

本次核对的 `main` HEAD：[`42784ffc83a72a516bfe952153ad7e2a3998d16c`](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/commit/42784ffc83a72a516bfe952153ad7e2a3998d16c)

## 结论速览

1. 单视频抖音解析应使用混合解析接口：`GET /api/hybrid/video_data`。
2. 请求至少传 `url`；`minimal` 可省略，默认 `false`。若调用方只需要下载字段，建议传 `minimal=true`。
3. `minimal=true` 的成功响应中，无水印高清视频字段是 `data.video_data.nwm_video_url_HQ`；普通无水印字段是 `data.video_data.nwm_video_url`。
4. 上游没有 API 层面的 Bearer/API-key 鉴权声明，也没有要求客户端把 Cookie 作为该接口参数发送。但服务端抓取抖音 Web API 时会使用部署配置中的 Douyin Cookie；README 明确要求部署者填入已登录账号的 Cookie。
5. `/api/download` 不是获取解析 JSON 的必经路径。它是服务端下载代理：再次解析传入的分享 URL，服务端抓取无水印 URL，保存后返回视频文件。对抖音直链能否在客户端直接播放取决于直链的有效期和请求头/风控；需要由上游服务代抓时再使用 `/api/download?url=...&with_watermark=false`。
6. README 对“直链可能 HTTP 403、应使用 `/api/download`”的明确警告针对 TikTok Web API。不能把这条 TikTok 说明扩大解释为“抖音 URL 必须永远经过 `/api/download`”。

## 1. 推荐 endpoint

推荐：

```text
GET /api/hybrid/video_data?url=<抖音分享链接或视频链接>&minimal=true
```

上游 README 的演示形式是：

```text
https://api.douyin.wtf/api/hybrid/video_data?url=[视频链接/Video URL]&minimal=false
```

其中 `url` 支持视频链接、分享链接或分享文本。源码路由的摘要明确称其为“混合解析单一视频接口”，并调用 `HybridCrawler.hybrid_parsing_single_video`。

`main` 把该路由注册在 `/api` 前缀下，`hybrid_parsing` 路由自身再使用 `/hybrid` 前缀、`/video_data` 路径，因此最终路径确实是 `/api/hybrid/video_data`。

## 2. 参数与响应

### `/api/hybrid/video_data`

OpenAPI 3.1（在线文档当前版本 `V4.1.2`）声明：

| 参数 | 位置 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `url` | query | 是 | 无 | 视频链接、分享链接或分享文本 |
| `minimal` | query | 否 | `false` | 是否返回上游整理后的最小数据 |

成功响应外层由 `ResponseModel` 约束：

```json
{
  "code": 200,
  "router": "/api/hybrid/video_data",
  "data": {}
}
```

OpenAPI 对 `data` 的类型只声明为任意 JSON，因此必须区分两种模式：

- `minimal=false`（默认）：源码先取抖音抓取结果的 `aweme_detail`，然后原样放入外层 `data`。它不是稳定的项目自定义最小 schema；视频原始对象中的播放数据位于 `video.play_addr`。
- `minimal=true`：源码把结果整理为下列结构。字段值如 `desc`、`author`、`statistics` 等来自抖音 `aweme_detail`，具体内容随视频变化。

```json
{
  "code": 200,
  "router": "/api/hybrid/video_data",
  "data": {
    "type": "video",
    "platform": "douyin",
    "video_id": "<aweme_id>",
    "desc": "<description>",
    "create_time": 0,
    "author": {},
    "music": {},
    "statistics": {},
    "cover_data": {
      "cover": "<url>",
      "origin_cover": "<url>",
      "dynamic_cover": "<url>"
    },
    "hashtags": [],
    "video_data": {
      "wm_video_url": "<watermarked-url>",
      "wm_video_url_HQ": "<watermarked-hq-url>",
      "nwm_video_url": "<no-watermark-url>",
      "nwm_video_url_HQ": "<no-watermark-hq-url>"
    }
  }
}
```

对单视频无水印下载，优先读取：

```text
data.video_data.nwm_video_url_HQ
```

回退字段为：

```text
data.video_data.nwm_video_url
```

实现上，抖音分支从 `data.video.play_addr.uri` 取得视频 URI，把 `data.video.play_addr.url_list[0]` 作为有水印 HQ 地址，并把地址中的 `playwm` 替换为 `play` 生成 `nwm_video_url_HQ`；同时生成带 `video_id` 的 `aweme.snssdk.com/aweme/v1/play/` 无水印地址作为 `nwm_video_url`。因此无水印 URL 不是客户端自己从原始 JSON 猜字段，而是 `minimal=true` 响应明确提供的字段。

### `/api/download`

OpenAPI 和路由源码声明的 query 参数：

| 参数 | 位置 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `url` | query | 是 | 无 | 抖音/TikTok/Bilibili 分享链接或视频/图片 URL |
| `prefix` | query | 否 | `true` | 是否使用下载文件名前缀 |
| `with_watermark` | query | 否 | `false` | 是否下载有水印版本 |

推荐的无水印下载调用形式：

```text
GET /api/download?url=<原始抖音分享链接>&prefix=true&with_watermark=false
```

这个接口的成功响应不是解析 JSON，而是视频文件响应（源码返回 `FileResponse`，媒体类型 `video/mp4`）。服务端流程是：

1. 用传入的原始 `url` 再执行一次 `hybrid_parsing_single_video(..., minimal=true)`。
2. 从 `data.video_data.nwm_video_url_HQ` 读取无水印 URL（`with_watermark=false` 时）。
3. 使用平台 crawler 生成的请求头在服务端流式抓取视频，落盘后返回文件。

如果服务端开关 `API.Download_Switch` 关闭，接口返回错误模型而不是文件；`main` 仓库的默认 `config.yaml` 中该开关为 `true`，但 README 说明演示站点为保护资源已关闭在线下载。

## 3. Cookie、鉴权与是否必须代理

### API 鉴权

在线 OpenAPI JSON 的根对象和这两个操作都没有 `security` 字段，也没有 Bearer、API-key 或 Cookie 参数声明。因此从该项目 API 合同看，客户端调用这两个 endpoint 不需要额外 API 鉴权头。

这不等于抖音上游不需要 Cookie。`DouyinWebCrawler.get_douyin_headers()` 从 `crawlers/douyin/web/config.yaml` 读取 `Cookie`，`fetch_one_video()` 用这些 headers 请求抖音详情接口。README 也明确要求部署者在配置中替换 Douyin 网站 Cookie，最好使用已登录账号的 Cookie，否则解析/API 可能不可用。

结论：Cookie 是部署端访问抖音上游的运行前提，通常不应由 ClipFlow 直接把 Cookie 转发给这个项目 API；API 服务端应自行维护 Cookie 配置。源码另提供 `/api/hybrid/update_cookie`，但这不是两个目标 endpoint 的必需参数，而且会把 Cookie 作为请求体发送，使用时需自行评估暴露风险。

### 是否必须经过 `/api/download`

- 不是解析层面的必经路径：`/api/hybrid/video_data?minimal=true` 已直接返回 `nwm_video_url_HQ`，调用方可以尝试使用该 URL。
- `/api/download` 是可选的服务端代理/落盘下载路径，适合客户端直连 URL 被风控、需要服务端携带请求头，或希望统一拿到 `video/mp4` 文件响应的场景。
- README 的明确 HTTP 403 警告针对 TikTok 直链；对抖音应把 `/api/download` 视为按实际直连结果选择的回退路线，而不是无条件要求。
- 解析 URL 和下载 URL 的输入都应优先使用原始抖音分享链接；`/api/download` 设计上会用它重新解析，不是把 `nwm_video_url_HQ` 再作为 `url` 传入。

## 4. 可引用的精确来源

以下源码链接全部固定到本次核对的 `main` commit `42784ffc83a72a516bfe952153ad7e2a3998d16c`：

- [README.md API 文档与演示参数（L317-L330）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/README.md#L317-L330)
- [README.md Cookie、演示站点与 `/api/download` 说明（L336-L343）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/README.md#L336-L343)
- [FastAPI 应用把 API router 注册为 `/api`（app/main.py L126-L139）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/app/main.py#L126-L139)
- [`/api/hybrid/video_data` 路由、参数和外层响应（hybrid_parsing.py L15-L53）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/app/api/endpoints/hybrid_parsing.py#L15-L53)
- [`ResponseModel` 外层 JSON（APIResponseModel.py L10-L24）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/app/api/models/APIResponseModel.py#L10-L24)
- [最小响应的抖音字段映射与 `nwm_video_url_HQ`（hybrid_crawler.py L101-L190）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/crawlers/hybrid/hybrid_crawler.py#L101-L190)
- [`/api/download` 参数、代理流程与无水印字段（download.py L111-L225）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/app/api/endpoints/download.py#L111-L225)
- [抖音 crawler 从配置读取 Cookie（web_crawler.py L70-L84）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/crawlers/douyin/web/web_crawler.py#L70-L84)
- [抖音详情请求使用上述 headers（endpoints.py L88-L110）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/crawlers/douyin/web/endpoints.py#L88-L110)
- [默认 API 文档、下载开关和文件配置（config.yaml L29-L42）](https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/config.yaml#L29-L42)

在线一手 OpenAPI 来源：

- [Swagger UI](https://douyin.wtf/docs)
- [OpenAPI JSON](https://douyin.wtf/openapi.json)

2026-08-04 核验结果：`https://douyin.wtf/docs` 和 `/openapi.json` 可访问，OpenAPI 为 `3.1.0`、项目版本 `V4.1.2`；README 中的旧地址 `https://api.douyin.wtf/docs`、`/openapi.json`、`/redoc` 返回 HTTP 404。在线文档属于运行中站点，后续可能漂移；需要复现本次结论时应优先使用上方 commit 固定的源码链接。
