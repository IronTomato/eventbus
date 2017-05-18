package irontomato.eventbus;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class EventBus {

    private Map<Class, Collection<EventListener>> registry = new HashMap<>();

    private Executor executor;

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

    public void remove(EventListener<?> listener) {
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

    public void onEvent(Event event) {
        selectListeners(event).forEach(l -> l.onEvent(event));

    }

    public void onEventAsync(Event event) {
        selectListeners(event).forEach(
                l -> executor.execute(
                        () -> l.onEvent(event)));
    }

    private Stream<EventListener> selectListeners(Event event) {
        return registry.keySet().stream()
                .filter(c -> c.isInstance(event))
                .flatMap(c -> registry.get(c).stream());
    }
}
