package irontomato.eventbus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventBus {

    private Map<Class, Collection<EventListener>> registry = new HashMap<>();

    private Executor executor;

    protected Logger logger = LogManager.getLogger(getClass());

    public EventBus() {
        executor = Executors.newCachedThreadPool();
    }

    public EventBus(Executor executor) {
        this.executor = executor;
    }

    public void register(EventListener<?> listener) {
        Arrays.stream(listener.getClass().getGenericInterfaces())
                .map(t -> (ParameterizedType) t)
                .filter(t -> t.getRawType() == EventListener.class)
                .map(t -> (Class) (t.getActualTypeArguments()[0]))
                .forEach(c -> {
                    if (registry.containsKey(c)) {
                        registry.get(c).add(listener);
                    } else {
                        registry.put(c, new ArrayList<>(Collections.singleton(listener)));
                    }
                });
    }

    public void unregister(EventListener<?> listener) {
        List<Class> emptyBuckets = new ArrayList<>();
        registry.entrySet().stream()
                .forEach(entry -> {
                    entry.getValue().remove(listener);
                    if (entry.getValue().isEmpty()) {
                        emptyBuckets.add(entry.getKey());
                    }
                });
        if (!emptyBuckets.isEmpty()) {
            emptyBuckets.forEach(c -> registry.remove(c));
        }
    }

    public <T extends Event> void clear(Class<T> eventClass) {
        registry.remove(eventClass);
    }

    public void onEvent(Event event) {
        selectListeners(event).forEach(
                l -> executor.execute(
                        () -> l.onEvent(event)));
    }

    public void onEvent(Event event, Runnable notifier) {
        List<EventListener> listeners = selectListeners(event).collect(Collectors.toList());
        CountDownLatch latch = new CountDownLatch(listeners.size());
        listeners.forEach(
                l -> executor.execute(
                        () -> {
                            try {
                                l.onEvent(event);
                            } finally {
                                latch.countDown();
                            }
                        }
                )
        );
        executor.execute(() -> {
            try {
                latch.await();
                notifier.run();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        });
    }

    public void onEvent(EventChain chain) {
        Event event = chain.pop();
        if (event == null) {
            return;
        }
        onEvent(event, () -> onEvent(chain));
    }

    private Stream<EventListener> selectListeners(Event event) {
        return registry.keySet().stream()
                .filter(c -> c.isInstance(event))
                .flatMap(c -> registry.get(c).stream());
    }
}
