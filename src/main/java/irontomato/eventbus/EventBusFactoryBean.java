package irontomato.eventbus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.Executor;

public class EventBusFactoryBean implements ApplicationContextAware, FactoryBean<EventBus> {

    private ApplicationContext context;

    private Executor executor;

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public EventBus getObject() throws Exception {
        EventBus bus = executor == null ? new EventBus() : new EventBus(executor);
        context.getBeansOfType(EventListener.class)
                .values()
                .forEach(bus::register);
        return bus;
    }

    @Override
    public Class<?> getObjectType() {
        return EventBus.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
