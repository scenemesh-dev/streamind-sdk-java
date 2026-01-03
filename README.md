# StreamInd SDK for Java

[![Maven](https://img.shields.io/badge/maven-central-green.svg)](https://search.maven.org/artifact/com.streamind/streamind-sdk)
[![Java](https://img.shields.io/badge/Java-11%2B-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

StreamInd Java SDK provides high-performance interfaces for connecting your Java applications to the StreamInd platform. Built with **Java 11+** for complete type safety and real-time bidirectional communication support.

## 🚀 Core Features

- 💎 **Full Type Safety**: Java generics and strong typing throughout
- ⚡ **High-Performance Concurrency**: CompletableFuture-based async operations
- 🔗 **Multi-Terminal Management**: One SDK instance manages multiple terminal connections (perfect for SaaS scenarios)
- 📡 **Bidirectional Communication**: Send signals to platform and receive directives
- 🌐 **WebSocket Transport**: Efficient real-time communication with TCP_NODELAY optimization
- 🎵 **Audio Streaming Support**: Binary audio data transmission (OPUS format)
- 🔄 **Auto-Reconnection**: Exponential backoff strategy with jitter algorithm
- 💓 **Heartbeat Keep-Alive**: Automatic connection maintenance
- 🛡️ **Comprehensive Error Handling**: Error callbacks, close callbacks, real-time exception capture
- 📊 **Connection Statistics**: Real-time monitoring of message send/receive, error counts, connection duration

## 📥 Installation

### Download JAR from GitHub Releases

1. Visit [Releases](https://github.com/scenemesh-dev/streamind-sdk-java/releases)
2. Download `streamind-sdk-1.0.0.jar`
3. Add to your project:

**Maven** - Place JAR in `libs/` directory and add to `pom.xml`:
```xml
<dependency>
    <groupId>com.streamind</groupId>
    <artifactId>streamind-sdk</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/streamind-sdk-1.0.0.jar</systemPath>
</dependency>
```

**Gradle** - Add to `build.gradle`:
```gradle
dependencies {
    implementation files('libs/streamind-sdk-1.0.0.jar')
}
```

## Quick Start

### Single Terminal Example

```java
import com.streamind.sdk.*;
import com.streamind.sdk.callbacks.*;

public class BasicExample {
    public static void main(String[] args) {
        // 1. Create configuration
        Config config = new Config.Builder(
            "my-device-001",           // deviceId
            "sensor",                  // deviceType
            "wss://platform.example.com/signals",  // endpoint
            "tenant-123",              // tenantId
            "product-456",             // productId
            "your-secret-key"          // productKey
        ).build();

        // 2. Create SDK instance
        StreamIndSDK sdk = new StreamIndSDK();

        // 3. Register terminal
        sdk.registerTerminal("terminal-1", config);

        // 4. Set directive callback
        sdk.setDirectiveCallback("terminal-1", directive -> {
            System.out.println("Received directive: " + directive.getName());
            int speed = directive.getIntParameter("speed", 100);
            // Handle directive...
        });

        // 5. Connect to platform
        ErrorCode result = sdk.connect("terminal-1");
        if (result != ErrorCode.OK) {
            System.err.println("Connection failed: " + sdk.getLastError());
            return;
        }

        // 6. Send signal
        Signal signal = new Signal("sensor.temperature");
        signal.getPayload().setNumber("celsius", 25.5);
        signal.getPayload().setString("location", "living_room");
        sdk.sendSignal("terminal-1", signal);

        // 7. Keep running
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 8. Cleanup
        sdk.shutdown();
    }
}
```

### Multi-Terminal Example (SaaS Scenario)

```java
import com.streamind.sdk.*;
import java.util.*;

public class MultiTerminalExample {
    public static void main(String[] args) {
        StreamIndSDK sdk = new StreamIndSDK();

        // Register multiple tenant terminals
        List<String> tenantIds = Arrays.asList("tenant-001", "tenant-002", "tenant-003");

        for (String tenantId : tenantIds) {
            Config config = new Config.Builder(
                tenantId + "-device",
                "saas-tenant",
                "wss://platform.example.com/signals",
                tenantId,
                "product-" + tenantId,
                "key-" + tenantId
            ).build();

            sdk.registerTerminal(tenantId, config);
        }

        // Connect all terminals concurrently (high performance!)
        Map<String, ErrorCode> results = sdk.connectAll();

        // Set global callback (automatically includes terminal_id)
        sdk.setGlobalDirectiveCallback((terminalId, directive) -> {
            System.out.println("Terminal " + terminalId + " received: " + directive.getName());
        });

        // Send signals to different terminals concurrently
        sdk.sendJSON("tenant-001", "user.login",
            Map.of("username", "alice", "timestamp", System.currentTimeMillis()));
        sdk.sendJSON("tenant-002", "data.update",
            Map.of("record_id", "12345", "action", "modify"));
        sdk.sendJSON("tenant-003", "alert.warning",
            Map.of("level", "high", "message", "Resource usage high"));

        // Query connection status
        List<String> connected = sdk.getConnectedTerminals();
        System.out.println("Connected terminals: " + connected);

        // Cleanup
        sdk.shutdown();
    }
}
```

## Core Concepts

### Terminal

**Terminal** is the core concept in the SDK, representing an independent WebSocket connection. In different scenarios, a terminal can be:

- **IoT Device**: Each physical device is a terminal
- **SaaS Tenant**: Each tenant is an independent terminal
- **Service Instance**: Each instance in a microservice architecture
- **User Session**: Each user connection is a terminal

### Signal (Uplink Message)

Signal is a message sent from a terminal to the platform.

```java
Signal signal = new Signal("sensor.temperature");

// Add data to payload
signal.getPayload().setNumber("celsius", 25.5);
signal.getPayload().setNumber("humidity", 60);
signal.getPayload().setString("location", "bedroom");
signal.getPayload().setBool("is_critical", false);

// Send to platform
sdk.sendSignal("terminal-id", signal);
```

### Directive (Downlink Message)

Directive is a command sent from the platform to a terminal.

```java
sdk.setDirectiveCallback("terminal-id", directive -> {
    // Get directive name
    String action = directive.getName();  // e.g., "motor.move"

    // Type-safe parameter extraction
    int speed = directive.getIntParameter("speed", 100);
    String direction = directive.getStringParameter("direction", "forward");
    boolean enabled = directive.getBoolParameter("enabled", true);

    // Execute business logic
    if ("motor.move".equals(action)) {
        motor.move(speed, direction);
    } else if ("led.blink".equals(action)) {
        int times = directive.getIntParameter("times", 3);
        led.blink(times);
    }
});
```

### Config (Configuration)

```java
Config config = new Config.Builder(
    "unique-device-id",                  // Device unique ID
    "sensor",                            // Device type
    "wss://api.example.com/signals",     // WebSocket URL
    "tenant-123",                        // Tenant ID
    "product-456",                       // Product ID
    "secret-key"                         // Authentication key
)
// Optional parameters with defaults
.enableDirectiveReceiving(true)
.connectionTimeoutMs(10000)
.heartbeatIntervalMs(5000)
.maxMessageSize(10 * 1024 * 1024)
.maxReconnectAttempts(-1)  // -1 = infinite reconnection
.build();
```

## SDK API Reference

### StreamIndSDK Class

#### Terminal Management

- `ErrorCode registerTerminal(String terminalId, Config config)` - Register a terminal
- `ErrorCode unregisterTerminal(String terminalId)` - Unregister a terminal
- `List<String> getAllTerminals()` - Get all registered terminal IDs
- `List<String> getConnectedTerminals()` - Get all connected terminal IDs

#### Connection Management

- `ErrorCode connect(String terminalId, String traceId)` - Connect specified terminal to platform
- `ErrorCode connect(String terminalId)` - Connect specified terminal (without trace ID)
- `Map<String, ErrorCode> connectAll()` - Connect all registered terminals concurrently
- `ErrorCode disconnect(String terminalId)` - Disconnect specified terminal
- `Map<String, ErrorCode> disconnectAll()` - Disconnect all terminals concurrently
- `boolean isConnected(String terminalId)` - Check if terminal is connected

#### Message Sending

- `ErrorCode sendSignal(String terminalId, Signal signal)` - Send signal through specified terminal
- `Map<Integer, ErrorCode> sendSignalsBatch(String terminalId, List<Signal> signals)` - Concurrently send multiple signals
- `ErrorCode sendAudioData(String terminalId, byte[] data, String audioFormat)` - Send audio data
- `ErrorCode sendAudioData(String terminalId, byte[] data)` - Send audio data (default: opus)
- `ErrorCode sendText(String terminalId, String signalType, String text)` - Convenience: Send text signal
- `ErrorCode sendJSON(String terminalId, String signalType, Map<String, Object> data)` - Convenience: Send JSON signal

#### Callbacks

**Single Terminal Callbacks**:
- `ErrorCode setConnectionCallback(String terminalId, ConnectionCallback callback)`
- `ErrorCode setDirectiveCallback(String terminalId, DirectiveCallback callback)`
- `ErrorCode setAudioDataCallback(String terminalId, AudioDataCallback callback)`
- `ErrorCode setErrorCallback(String terminalId, ErrorCallback callback)`
- `ErrorCode setCloseCallback(String terminalId, CloseCallback callback)`

**Global Callbacks** (automatically includes terminal_id parameter):
- `void setGlobalConnectionCallback(GlobalConnectionCallback callback)`
- `void setGlobalDirectiveCallback(GlobalDirectiveCallback callback)`
- `void setGlobalErrorCallback(GlobalErrorCallback callback)`
- `void setGlobalCloseCallback(GlobalCloseCallback callback)`

#### Statistics & Monitoring

- `Statistics getTerminalStatistics(String terminalId)` - Get statistics for specified terminal
- `Map<String, Statistics> getAllStatistics()` - Get statistics for all terminals

#### Other

- `String getLastError()` - Get last error message
- `void clearError()` - Clear error message
- `static String getVersion()` - Get SDK version
- `void shutdown()` - Shutdown SDK (cleanup all resources)

## High-Performance Features

### 1. Concurrent Batch Sending

```java
// Send 100 signals simultaneously
List<Signal> signals = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    signals.add(new Signal("test_" + i));
}
Map<Integer, ErrorCode> results = sdk.sendSignalsBatch("terminal-1", signals);

// Typical performance: 100 signals < 100ms
```

### 2. Multi-Terminal Concurrent Operations

```java
// Connect 1000 terminals simultaneously
Map<String, ErrorCode> results = sdk.connectAll();

// The SDK uses CompletableFuture for efficient concurrent operations
```

### 3. TCP Optimization

- **TCP_NODELAY**: Disable Nagle's algorithm, send small packets immediately
- **Efficient WebSocket**: Using Java-WebSocket library for high performance
- **Real-Time Transmission**: Messages immediately enter network layer

## Audio Streaming

### Sending Audio Data

```java
// Set audio callback
sdk.setAudioDataCallback("terminal-id", audioData -> {
    System.out.println("Received " + audioData.length + " bytes of audio");
    // Play audio or save to file
    audioPlayer.play(audioData, "opus");
});

// Send audio data
byte[] opusAudioData = microphone.read();
sdk.sendAudioData("terminal-id", opusAudioData, "opus");
```

## Error Handling

### Return Value Checking

```java
ErrorCode result = sdk.sendSignal("terminal-id", signal);

if (result == ErrorCode.OK) {
    System.out.println("Signal sent successfully");
} else if (result == ErrorCode.NOT_CONNECTED) {
    System.out.println("Terminal not connected");
} else if (result == ErrorCode.SIGNAL_TOO_LARGE) {
    System.out.println("Signal exceeds maximum size limit");
} else {
    System.out.println("Error: " + sdk.getLastError());
}
```

### Error Callback (Real-Time Monitoring)

```java
// Single terminal error callback
sdk.setErrorCallback("terminal-id", (errorCode, message) -> {
    System.err.println("Error [" + errorCode + "]: " + message);
});

// Global error callback (all terminals)
sdk.setGlobalErrorCallback((terminalId, errorCode, message) -> {
    System.err.println("Terminal " + terminalId + " error [" + errorCode + "]: " + message);
    // Take action based on error type
    if (errorCode == ErrorCode.CONNECTION_FAILED) {
        // Log, send alert, etc.
    }
});
```

## Error Codes

| Error Code | Description |
|------|---------|
| `OK` | Success |
| `NOT_INITIALIZED` | SDK not initialized |
| `ALREADY_INITIALIZED` | SDK already initialized |
| `INVALID_CONFIG` | Invalid configuration |
| `NOT_CONNECTED` | Not connected to platform |
| `CONNECTION_FAILED` | Connection failed |
| `CONNECTION_TIMEOUT` | Connection timeout |
| `INVALID_SIGNAL` | Invalid signal |
| `SIGNAL_TOO_LARGE` | Signal exceeds maximum size |
| `SEND_FAILED` | Send failed |
| `INVALID_PARAMETER` | Invalid parameter |
| `TERMINAL_NOT_FOUND` | Terminal not found |

## System Requirements

- Java 11 or higher
- Maven 3.6+ or Gradle 6.0+
- Dependencies:
  - Java-WebSocket 1.5.5
  - Gson 2.10.1
  - SLF4J 2.0.9

## Building from Source

```bash
# Clone the repository
git clone https://github.com/streamind/streamind-sdk-java.git
cd streamind-sdk-java

# Build with Maven
mvn clean package

# The JAR file will be in target/streamind-sdk-1.0.0.jar
```

## License

Proprietary License - See [LICENSE](LICENSE) file for details

## Support

For questions, suggestions, or contributions:

- GitHub Issues: https://github.com/streamind/streamind-sdk-java/issues
- Email: support@streamind.com
- Documentation: https://docs.streamind.com

## Changelog

### Version 1.0.0 (2025-01-02)

**Core Architecture**:
- 💎 **Full Java 11+ Support** - Strong typing and modern Java features
- ⚡ **High-Performance Optimization** - TCP_NODELAY, concurrent batch operations with CompletableFuture
- 🔗 **Multi-Terminal Management** - Support massive concurrent connections
- 📡 **WebSocket Bidirectional Communication** - Real-time Signal/Directive
- 🎵 **Audio Streaming Support** - OPUS format binary transmission with 14-byte application-layer protocol
- 🔄 **Intelligent Reconnection** - Exponential backoff + jitter algorithm
- 💓 **Heartbeat Keep-Alive** - Automatic connection maintenance
- 🛡️ **Comprehensive Error Handling** - Error callbacks, close callbacks
- 📊 **Statistics Monitoring** - Real-time connection statistics
- ✨ **Convenience Methods** - sendText, sendJSON simplify common operations
