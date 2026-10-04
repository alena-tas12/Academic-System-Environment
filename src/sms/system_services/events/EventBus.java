package sms.system_services.events;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
    private static EventBus instance;
    private Map<Class<?>, List<EventListener>> listeners = new HashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        if (instance == null) instance = new EventBus();
        return instance;
    }

    public void subscribe(Class<?> eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(Object event) {
        System.out.println("[EventBus] Publishing event: " + event.getClass().getSimpleName());
        
        // Notify direct matches
        notifyListeners(event.getClass(), event);
        
        // Notify catch-all matches (Object.class)
        notifyListeners(Object.class, event);
    }
    
    private void notifyListeners(Class<?> type, Object event) {
        List<EventListener> eventListeners = listeners.get(type);
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }
}
