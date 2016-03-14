package atlantis.com.atlantis.utils;

/**
 * Contains constants for events
 * Created by ricardo on 5/10/15.
 */
public class Events {

    private static final String EVENTS_NAMESPACE = "atlantis.com.atlantis.events";

    public static final String MESSAGE_CREATED      = EVENTS_NAMESPACE + ".MESSAGE_CREATED";
    public static final String OTP_SYNCED           = EVENTS_NAMESPACE + ".OTP_SYNCED";
    public static final String CONVERSATION_CREATED = EVENTS_NAMESPACE + ".CONVERSATION_CREATED";
    public static final String CONVERSATION_DELETED = EVENTS_NAMESPACE + ".CONVERSATION_DELETED";
    public static final String MESSAGE_DELETED      = EVENTS_NAMESPACE + ".MESSAGE_DELETED";
    public static final String MESSAGE_DELIVERED    = EVENTS_NAMESPACE + ".MESSAGE_DELIVERED";
    public static final String NOTEBOOK_CREATED     = EVENTS_NAMESPACE + ".NOTEBOOK_CREATED";
    public static final String NOTEBOOK_DELETED     = EVENTS_NAMESPACE + ".NOTEBOOK_DELETED";
    public static final String SERVICE_BOUND        = EVENTS_NAMESPACE + ".SERVICE_BOUND";
}
