package io.harness.fme.demo.flags;

import org.springframework.stereotype.Service;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FeatureFlagService {

    private static final int INTERVAL_SECONDS = 1;
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(
                    Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                    r -> {
                        Thread t = new Thread(r, "ff-poller");
                        t.setDaemon(true);
                        return t;
                    });

    private final ConcurrentMap<String, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    public SplitClient splitClient;

    public FeatureFlagService() {
        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(10000)
                .impressionsRefreshRate(5)
                .impressionsMode(io.split.client.impressions.ImpressionsManager.Mode.DEBUG)
                .build();

        try {
            String key = System.getenv("HARNESS_SERVER_SIDE_API_KEY");
            SplitFactory splitFactory = SplitFactoryBuilder.build(key, config);
            this.splitClient = splitFactory.client();
            this.splitClient.blockUntilReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("FeatureFlagService initialized");
    }

    public String getTreatment(String key, String flagName) {
        return this.splitClient.getTreatment(key, flagName);
    }

    // Overload with optional attributes
    public void notifyOnUpdate(ITreatmentUpdate callback, String key, String flagName) {
        notifyOnUpdate(callback, key, flagName, null);
    }

    public void notifyOnUpdate(ITreatmentUpdate callback,
                               String key,
                               String flagName,
                               Map<String, Object> attributes) {

        Objects.requireNonNull(callback, "callback must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(flagName, "flagName must not be null");

        final Map<String, Object> attrs = (attributes == null) ? Map.of() : attributes;

        // Unique subscription id per (key, flagName, callback)
        final String subId = buildSubscriptionId(key, flagName, callback);

        // If there's already a poll running for this triple, don't start another
        subscriptions.computeIfAbsent(subId, id -> {
            // Fetch initial treatment
            final AtomicReference<String> current = new AtomicReference<>(
                    splitClient.getTreatment(key, flagName, attrs)
            );

            // Schedule the poll
            return SCHEDULER.scheduleAtFixedRate(() -> {
                try {
                    String next = splitClient.getTreatment(key, flagName, attrs);
                    String prev = current.get();

                    if (!Objects.equals(prev, next)) {
                        current.set(next);
                        // fire callback on change
                        try {
                            callback.update(flagName, prev, next);
                        } catch (Throwable cbEx) {
                            // Don't let callback exceptions kill the scheduler thread
                            cbEx.printStackTrace();
                        }
                    }
                } catch (Throwable pollEx) {
                    // Log and keep going; transient failures shouldn’t stop polling
                    pollEx.printStackTrace();
                }
            }, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
        });
    }

    private String buildSubscriptionId(String key, String flagName, ITreatmentUpdate cb) {
        // include callback identity so multiple callbacks can watch the same flag/key independently
        return key + "||" + flagName + "||" + System.identityHashCode(cb);
    }

    /** Optionally allow callers to cancel a specific subscription */
    public boolean cancelNotifyOnUpdate(ITreatmentUpdate callback, String key, String flagName) {
        String id = buildSubscriptionId(key, flagName, callback);
        ScheduledFuture<?> f = subscriptions.remove(id);
        if (f != null) {
            return f.cancel(true);
        }
        return false;
    }

    // Shutdown the client and all pollers
    public void shutdown() {
        if (this.splitClient != null) {
            this.splitClient.destroy();
        }
        // Cancel all outstanding polls
        for (ScheduledFuture<?> f : subscriptions.values()) {
            try {
                f.cancel(true);
            } catch (Throwable ignored) {}
        }
        subscriptions.clear();
        SCHEDULER.shutdownNow();
    }
}
