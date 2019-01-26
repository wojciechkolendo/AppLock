package com.wojciechkolendo.applock.events;

/**
 * AppListForegroundEvent
 */

public class AppListForegroundEvent {
    private boolean isForeground = false;

    public AppListForegroundEvent(boolean isForeground) {
        this.isForeground = isForeground;
    }

    public boolean isForeground() {
        return isForeground;
    }
}
