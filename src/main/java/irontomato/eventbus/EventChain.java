package irontomato.eventbus;

public interface EventChain {
    void push(Event event);

    Event pop();
}
