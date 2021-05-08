# vertx-ignite-streamer

vertx-ignite-streamer provides streaming events from Vert.x event bus to key-value pairs into Apache Ignite instance.

## Usage
### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.ivpal:vertx-ignite-streamer:1.0.0-SNAPSHOT'
}
```
### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ivpal</groupId>
    <artifactId>vertx-ignite-streamer</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Java code example:
```java
var config = new IgniteConfiguration();
var clusterManager = new IgniteClusterManager(config);
var vertxOptions = new VertxOptions().setClusterManager(clusterManager);
final var cacheName = "words";
final var address = "address";

Vertx.clusteredVertx(vertxOptions)
    .onSuccess(vertx -> {
        var ignite = clusterManager.getIgniteInstance();
        ignite.createCache(cacheName);
        var streamer = ignite.<String, String>dataStreamer(cacheName);
        streamer.allowOverwrite(true);
        streamer.perThreadBufferSize(1);
        streamer.autoFlushFrequency(1000);

        var vertxStreamer = new VertxStreamer<String, String, String>();
        vertxStreamer.setAddress(address);
        vertxStreamer.setVertx(vertx);
        vertxStreamer.setIgnite(ignite);
        vertxStreamer.setStreamer(streamer);
        vertxStreamer.setSingleTupleExtractor(msg -> new Map.Entry<>() {
            @Override
            public String getKey() {
                return msg.split(":")[0];
            }

            @Override
            public String getValue() {
                return msg.split(":")[1];
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        });
        vertxStreamer.start();

        vertx.eventBus().send(address, "key:value");

        // Because we set autoFlushFrequency to 1000
        vertx.setTimer(1000, event -> {
            var value = ignite.<String, String>cache(cacheName).get("key"); // should be "value"
        });
    });
```