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
```

```json
{
  "mcpServers": {
    "sequential-thinking": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-sequential-thinking"
      ]
    },
    "bing-search": {
      "args": [
        "-y",
        "bing-cn-mcp"
      ],
      "command": "npx"
    },
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=Asia/Shanghai"
      ]
    },
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest"
      ]
    },
    "fetch": {
      "args": [
        "mcp-server-fetch"
      ],
      "command": "uvx"
    },
    "us-weather": {
      "args": [
        "-jar",
        "mcp/us-weather/target/us-weather-0.0.1-SNAPSHOT.jar"
      ],
      "command": "java"
    },
    "cn-weather": {
      "args": [
        "-jar",
        "mcp/cn-weather/target/cn-weather-0.0.1-SNAPSHOT.jar"
      ],
      "command": "java"
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
