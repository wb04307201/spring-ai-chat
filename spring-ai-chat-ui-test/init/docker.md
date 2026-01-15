## chroma
```shell
docker run -d --name redis-stack -p 9379:6379 -e REDIS_ARGS="--requirepass 123456" redis/redis-stack:latest
```

```shell
pip install uv
npm install -g npx
```

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=America/New_York"
      ]
    }
  }
}
```

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": [
        "@playwright/mcp@latest"
      ]
    }
  }
}
```
