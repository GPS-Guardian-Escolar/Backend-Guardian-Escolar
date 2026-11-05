package com.guardianescolar.api.modules.notifications.domain;

public final class NotificationEventTypes {

    public static final String DELAY = "DELAY";
    public static final String INACTIVITY = "INACTIVITY";
    public static final String LOCATION_UNAVAILABLE = "LOCATION_UNAVAILABLE";
    public static final String LOCATION_STOPPED = "LOCATION_STOPPED";
    public static final String ROUTE_DEVIATION = "ROUTE_DEVIATION";
    public static final String SAFE_ZONE_ENTRY = "SAFE_ZONE_ENTRY";
    public static final String SAFE_ZONE_EXIT = "SAFE_ZONE_EXIT";
    public static final String TRIP_STARTED = "TRIP_STARTED";
    public static final String TRIP_FINISHED = "TRIP_FINISHED";
    public static final String DEVICE_LINKED = "DEVICE_LINKED";

    private NotificationEventTypes() {
    }
}
