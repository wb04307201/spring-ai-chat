## redis-stack
```shell
docker run -d --name redis-stack -p 9379:6379 -e REDIS_ARGS="--requirepass 123456" redis/redis-stack:latest
```

## Qdrant
```shell
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant:latest
```
`http://localhost:6333/dashboard`

```shell
pip install uv
npm install -g npx
where uv
where npx
# linux/mac
curl -Ls https://sh.jbang.dev | bash -s - app setup
# windows powershell
iex "& { $(iwr https://ps.jbang.dev) } app setup"
```

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=Asia/Shanghai"
      ]
    },
    "bing-search": {
      "args": [
        "-y",
        "bing-cn-mcp"
      ],
      "command": "npx"
    },
    "fetch": {
      "args": [
        "mcp-server-fetch"
      ],
      "command": "uvx"
    },
    "sequential-thinking": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-sequential-thinking"
      ]
    },
    "cn-weather-mcp": {
      "args": [
        "io.github.wb04307201:cn-weather-mcp:1.0.0"
      ],
      "command": "jbang"
    },
    "amap-maps": {
      "args": [
        "-y",
        "@amap/amap-maps-mcp-server"
      ],
      "command": "npx",
      "env": {
        "AMAP_MAPS_API_KEY": ""
      }
    },
    "12306-mcp": {
      "args": [
        "-y",
        "12306-mcp"
      ],
      "command": "npx"
    },
    "mcp-server-chart": {
      "args": [
        "-y",
        "@antv/mcp-server-chart"
      ],
      "command": "npx"
    },
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest"
      ]
    },
    "chrome-devtools": {
      "args": [
        "chrome-devtools-mcp@latest"
      ],
      "command": "npx"
    },
    "tavily-mcp": {
      "args": [
        "-y",
        "tavily-mcp@0.1.4"
      ],
      "autoApprove": [],
      "command": "npx",
      "disabled": false,
      "env": {
        "TAVILY_API_KEY": "your-api-key-here"
      }
    }
  }
}
```
