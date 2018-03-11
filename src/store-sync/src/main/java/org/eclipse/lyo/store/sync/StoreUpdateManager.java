package org.eclipse.lyo.store.sync;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.lyo.store.Store;
import org.eclipse.lyo.store.sync.change.Change;
import org.eclipse.lyo.store.sync.change.ChangeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules {@link StoreUpdateRunnable} via internal
 * {@link ScheduledExecutorService} with a predefined delay or on-demand.
 * Operates on a generic message that is passed to handlers.
 *
 * @author Andrew Berezovskyi (andriib@kth.se)
 * @version $version-stub$
 * @since 0.0.0
 */
public class StoreUpdateManager<M> {
    private final static Logger LOGGER = LoggerFactory.getLogger(StoreUpdateManager.class);
    private static final int NODELAY = 0;
    private static final int SINGLE_THREAD_POOL = 1;
    private final ScheduledExecutorService executor;
    private final ChangeProvider<M> changeProvider;
    private final Store store;
    private final List<Handler<M>> handlers = new ArrayList<>();

    /**
     * Schedules {@link StoreUpdateRunnable} via internal
     * {@link ScheduledExecutorService} with a predefined delay or on-demand.
     *
     * @param store
     *            Instance of an initialised Store
     * @param changeProvider
     *            Provider of the changes in the underlying tool.
     */
    public StoreUpdateManager(final Store store, final ChangeProvider<M> changeProvider) {
        executor = Executors.newScheduledThreadPool(StoreUpdateManager.SINGLE_THREAD_POOL);
        this.store = store;
        this.changeProvider = changeProvider;
    }

    /**
     * Polling update on a given {@link ChangeProvider} followed by a notification
     * to all previously registered {@link Handler} objects.
     *
     * @param lastUpdate
     *            Time of last store update, changes before this moment might be
     *            dropped.
     * @param delaySeconds
     *            Seconds between polling checks.
     */
    public void poll(final ZonedDateTime lastUpdate, final int delaySeconds) {
        final StoreUpdateRunnable<M> updateRunnable = buildRunnable(store, changeProvider, lastUpdate, null, handlers);
        executor.scheduleWithFixedDelay(updateRunnable, StoreUpdateManager.NODELAY, delaySeconds, TimeUnit.SECONDS);

        StoreUpdateManager.LOGGER.trace("Poll request has been enqueued");
    }

    /**
     * Submit a single update request. Typically done from the HTTP handler.
     *
     * @param lastUpdate
     * @param message
     *            Specific details for the {@link ChangeProvider}
     * @return {@link Future} that allows to block until the runnable is finished
     *         executing (strongly discouraged).
     */
    public Future<?> submit(final ZonedDateTime lastUpdate, final M message) {
        final StoreUpdateRunnable<M> updateRunnable = buildRunnable(store, changeProvider, lastUpdate, message,
                handlers);
        return executor.submit(updateRunnable);
    }

    /**
     * Add a {@link Handler} that will process the collection of {@link Change}
     * generated by the {@link ChangeProvider}. Handler is not guaranteed to be
     * called if added after the update has been scheduled.
     *
     * @param handler
     *            Handler that should be called whenever {@link ChangeProvider}
     *            returns any changes.
     */
    public void addHandler(final Handler<M> handler) {
        handlers.add(handler);
    }

    /**
     * Method can be overridden in case a more sophisticated Runnable has to be
     * constructed.
     */
    protected StoreUpdateRunnable<M> buildRunnable(final Store store, final ChangeProvider<M> changeProvider,
            final ZonedDateTime lastUpdate, final M message, final List<Handler<M>> handlers) {
        return new StoreUpdateRunnable<>(store, changeProvider, lastUpdate, message, handlers);
    }
}
