package com.github.tornaia.jimglabel.common.event;

public abstract class AbstractEvent {

    private final long timestamp;

    protected AbstractEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
