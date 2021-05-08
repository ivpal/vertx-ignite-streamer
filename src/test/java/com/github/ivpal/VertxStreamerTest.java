package com.github.ivpal;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class VertxStreamerTest {
    @Test
    void testStreamer() throws InterruptedException {
        var testContext = new VertxTestContext();
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
                vertx.setTimer(1000, event -> {
                    var value = ignite.<String, String>cache(cacheName).get("key");
                    assertThat(value).isEqualTo("value");
                    testContext.completeNow();
                });
            });

        assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
    }
}
