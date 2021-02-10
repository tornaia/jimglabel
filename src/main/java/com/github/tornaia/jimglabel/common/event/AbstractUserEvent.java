package com.github.tornaia.jimglabel.common.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractUserEvent extends AbstractEvent {

    protected AbstractUserEvent(long timestamp) {
        super(timestamp);
    }

    @JsonIgnore
    public boolean isPersistable() {
        return true;
    }
}
