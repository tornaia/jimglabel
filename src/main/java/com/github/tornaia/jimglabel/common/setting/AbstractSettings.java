package com.github.tornaia.jimglabel.common.setting;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public abstract class AbstractSettings implements Serializable {

    private static final long serialVersionUID = 2664985435093790102L;

    @SuppressWarnings("unchecked")
    public <T extends AbstractSettings> T copy() {
        return (T) SerializationUtils.clone(this);
    }
}
