# StreamInd SDK for Java

StreamInd Java SDK - 基于Java 11+的高性能WebSocket SDK，用于连接StreamInd平台。

## 安装

1. 访问 [Releases](https://github.com/scenemesh-dev/streamind-sdk-java/releases)
2. 下载 `streamind-sdk-1.0.0.jar`
3. 添加到项目：

**Maven** - 将JAR放到 `libs/` 目录，在 `pom.xml` 添加：
```xml
<dependency>
    <groupId>com.streamind</groupId>
    <artifactId>streamind-sdk</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/streamind-sdk-1.0.0.jar</systemPath>
</dependency>
```

**Gradle** - 在 `build.gradle` 添加：
```gradle
dependencies {
    implementation files('libs/streamind-sdk-1.0.0.jar')
}
```

## 快速开始

```java
import com.streamind.sdk.*;
import com.streamind.sdk.callbacks.*;

public class Example {
    public static void main(String[] args) {
        // 1. 创建配置
        Config config = new Config.Builder(
            "device-001",                           // deviceId
            "sensor",                               // deviceType
            "wss://your-platform.com/signals",     // endpoint
            "your-tenant-id",                       // tenantId
            "your-product-id",                      // productId
            "your-secret-key"                       // productKey
        ).build();

        // 2. 创建SDK并注册终端
        StreamIndSDK sdk = new StreamIndSDK();
        sdk.registerTerminal("terminal-1", config);

        // 3. 设置回调
        sdk.setDirectiveCallback("terminal-1", directive -> {
            System.out.println("收到指令: " + directive.getName());
        });

        // 4. 连接
        sdk.connect("terminal-1");

        // 5. 发送信号
        Signal signal = new Signal("sensor.data");
        signal.getPayload().setNumber("value", 25.5);
        sdk.sendSignal("terminal-1", signal);
    }
}
```

## 发送音频数据

```java
// 读取OPUS音频文件
byte[] audioData = Files.readAllBytes(Paths.get("audio.opus"));

// 发送音频
sdk.sendAudioData("terminal-1", audioData);
```

## 多终端管理

```java
// 注册多个终端
sdk.registerTerminal("terminal-1", config1);
sdk.registerTerminal("terminal-2", config2);

// 批量连接
Map<String, ErrorCode> results = sdk.connectAll();

// 批量发送
sdk.sendSignal("terminal-1", signal1);
sdk.sendSignal("terminal-2", signal2);
```

## API参考

### StreamIndSDK类

| 方法 | 说明 |
|-----|------|
| `registerTerminal(terminalId, config)` | 注册终端 |
| `connect(terminalId)` | 连接终端 |
| `sendSignal(terminalId, signal)` | 发送信号 |
| `sendAudioData(terminalId, data)` | 发送音频（OPUS格式） |
| `setDirectiveCallback(terminalId, callback)` | 设置指令回调 |
| `setConnectionCallback(terminalId, callback)` | 设置连接状态回调 |
| `disconnect(terminalId)` | 断开连接 |

### Config配置

| 参数 | 类型 | 说明 |
|-----|------|------|
| `deviceId` | String | 设备ID |
| `deviceType` | String | 设备类型 |
| `endpoint` | String | WebSocket端点 |
| `tenantId` | String | 租户ID |
| `productId` | String | 产品ID |
| `productKey` | String | 产品密钥 |

### Config.Builder可选配置

```java
Config config = new Config.Builder(...)
    .heartbeatIntervalMs(30000)         // 心跳间隔（默认30秒）
    .connectionTimeoutMs(10000)         // 连接超时（默认10秒）
    .maxReconnectAttempts(10)           // 最大重连次数（默认10次）
    .build();
```

## 要求

- Java 11+
- Maven或Gradle

## 许可证

Proprietary License
