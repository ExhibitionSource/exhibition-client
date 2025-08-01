package exhibition.event;

import exhibition.event.impl.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* Used for registering classes for event handling, and dispatching events to
 * those registered classes.<br>
 * <br>
 * Event usage:
 *
 * <pre>
 * EventSystem.getInstance(EventExample.class).fire();
 * // Or give event-specific data by using casting
 * ((EventExample) EventSystem.getInstance(EventExample.class)).fire(generic_info);
 * </pre>
 */
public class EventSystem {
    private static final HashMap<Class<Event>, EventSubscription> registry = new HashMap<>();

    // For events that aren't called on separate threads
    private static final HashMap<Class, Event> instances = new HashMap<>();

    /*
      Sets up the instances map.
     */
    static {
        EventSystem.instances.put(EventLiquidCollide.class, new EventLiquidCollide());
        EventSystem.instances.put(EventStep.class, new EventStep());
        EventSystem.instances.put(EventDamageBlock.class, new EventDamageBlock());
        EventSystem.instances.put(EventPushBlock.class, new EventPushBlock());
        EventSystem.instances.put(EventTick.class, new EventTick());
        EventSystem.instances.put(EventDeath.class, new EventDeath());
        EventSystem.instances.put(EventMouse.class, new EventMouse());
        EventSystem.instances.put(EventRender3D.class, new EventRender3D());
        EventSystem.instances.put(EventRenderGui.class, new EventRenderGui());
        EventSystem.instances.put(EventScreenDisplay.class, new EventScreenDisplay());
        EventSystem.instances.put(EventAttack.class, new EventAttack());
        EventSystem.instances.put(EventPacket.class, new EventPacket());
        EventSystem.instances.put(EventVelocity.class, new EventVelocity());
        EventSystem.instances.put(EventMotionUpdate.class, new EventMotionUpdate());
        EventSystem.instances.put(EventChat.class, new EventChat());
        EventSystem.instances.put(EventBlockBounds.class, new EventBlockBounds());
        EventSystem.instances.put(EventNametagRender.class, new EventNametagRender());
        EventSystem.instances.put(EventMove.class, new EventMove());
        EventSystem.instances.put(EventKeyPress.class, new EventKeyPress());
        EventSystem.instances.put(EventRenderEntity.class, new EventRenderEntity());
        EventSystem.instances.put(EventSpawnEntity.class, new EventSpawnEntity());
        EventSystem.instances.put(EventSpawnPlayer.class, new EventSpawnPlayer());
        EventSystem.instances.put(EventRenderGuiLast.class, new EventRenderGuiLast());
        EventSystem.instances.put(EventRenderPreScreen.class, new EventRenderPreScreen());
    }

    /**
     * Registers a listener for event handling.
     *
     * @param listener
     */
    public static void register(EventListener listener) {
        List<Class<Event>> events = EventSystem.getEvents(listener);
        for (Class<Event> event : events) {
            if (EventSystem.isEventRegistered(event)) {
                EventSubscription subscription = EventSystem.registry.get(event);
                subscription.add(listener);
            } else {
                EventSubscription subscription = new EventSubscription();
                subscription.add(listener);
                EventSystem.registry.put(event, subscription);
            }
        }
    }

    /**
     * Unregisters a listener for event handling.
     *
     * @param listener
     */
    public static void unregister(EventListener listener) {
        List<Class<Event>> events = EventSystem.getEvents(listener);
        for (Class<Event> event : events) {
            if (EventSystem.isEventRegistered(event)) {
                EventSubscription sub = EventSystem.registry.get(event);
                sub.remove(listener);
            }
        }
    }

    /**
     * Fires an event. The event is sent to every registered listener that has
     * requested it via an @RegisterEvent annotation.
     *
     * @param event
     * @return
     */
    public static Event fire(Event event) {
        EventSubscription subscription = EventSystem.registry.get(event.getClass());
        if (subscription != null) {
            subscription.fire(event);
        }
        return event;
    }

    /**
     * Retrieves an instance of an event given its class.
     *
     * @param eventClass
     * @return
     */
    public static <T extends Event> T getInstance(Class<T> eventClass) {
        return (T) EventSystem.instances.get(eventClass);
    }

    /**
     * Gets the events requested by a listener.
     *
     * @param listener
     * @return
     */
    private static List<Class<Event>> getEvents(EventListener listener) {
        ArrayList<Class<Event>> events = new ArrayList<>();
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(RegisterEvent.class)) {
                continue;
            }
            RegisterEvent ireg = method.getAnnotation(RegisterEvent.class);
            for (Class eventClass : ireg.events()) {
                events.add(eventClass);
            }
        }
        return events;
    }

    /**
     * Checks if the event is in the registry.
     *
     * @param event
     * @return
     */
    private static boolean isEventRegistered(Class<Event> event) {
        return EventSystem.registry.containsKey(event);
    }
}
