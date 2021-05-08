package com.github.ivpal;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.stream.StreamAdapter;

/**
 * Server that consumes events from Vert.x event bus and streams its to to key-value pairs into
 * {@link IgniteDataStreamer} instance.
 */
public class VertxStreamer<T, K, V> extends StreamAdapter<T, K, V> {
    /** Vert.x instance. */
    private Vertx vertx;

    /** Address for consume events. */
    private String address;

    /** MessageConsumer instance. */
    private MessageConsumer<T> consumer;

    /** Logger. */
    private IgniteLogger log;

    /**
     * Sets the Vert.x instance.
     *
     * @param vertx Vert.x instance.
     */
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Sets the address for consume events.
     *
     * @param address Address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Starts streamer.
     *
     * @throws IgniteException If failed.
     */
    public void start() {
        A.notNull(getStreamer(), "streamer");
        A.notNull(getIgnite(), "ignite");
        A.notNull(address, "address");
        A.notNull(vertx, "vertx");

        A.ensure(
            null != getSingleTupleExtractor() || null != getMultipleTupleExtractor(),
            "Extractor must be configured"
        );

        log = getIgnite().log();

        consumer = vertx.eventBus().<T>consumer(address).handler(event -> {
            try {
                addMessage(event.body());
            } catch (Exception ex) {
                U.error(log, "Event is ignored due to an error [event = " + event.body() + ']', ex);
            }
        }).exceptionHandler(ex -> {
            U.error(log, "Exception occurs during processing events", ex);
        });
    }

    /**
     * Stops streamer.
     *
     * @return Future.
     */
    public Future<Void> stop() {
        return consumer.unregister();
    }
}
