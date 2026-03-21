/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package cn.wubo.spring.ai.mcp.cn.weather;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

import java.nio.file.Paths;
import java.util.Map;

/**
 * With stdio transport, the MCP server is automatically started by the client. But you
 * have to build the server jar first:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
public class ClientStdio {

	public static void main(String[] args) {

		// 获取用户主目录或使用绝对路径
		String userHome = System.getProperty("user.home");
		String jarPath = Paths.get(userHome, "..", "developer", "IdeaProjects", "spring-ai-chat", "mcp", "weather", "target",
				"weather-0.0.1-SNAPSHOT.jar").toAbsolutePath().toString();
		
		// 或者直接指定绝对路径 (根据你的实际路径调整)
		// String jarPath = "D:\\developer\\IdeaProjects\\spring-ai-chat\\mcp\\weather\\target\\weather-0.0.1-SNAPSHOT.jar";
		
		System.out.println("Looking for JAR at: " + jarPath);
		if (!java.nio.file.Files.exists(java.nio.file.Paths.get(jarPath))) {
			System.err.println("ERROR: JAR file not found at: " + jarPath);
			System.exit(1);
		}

		var stdioParams = ServerParameters.builder("java")
				.args("-jar", jarPath)
				.build();

		var transport = new StdioClientTransport(stdioParams, McpJsonMapper.createDefault());
		var client = McpClient.sync(transport).build();

		client.initialize();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
		System.out.println("Available Tools = " + toolsList);

		CallToolResult weatherForcastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
				Map.of("latitude", "47.6062", "longitude", "-122.3321")));
		System.out.println("Weather Forcast: " + weatherForcastResult);

		CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
		System.out.println("Alert Response = " + alertResult);

		client.closeGracefully();
	}

}
