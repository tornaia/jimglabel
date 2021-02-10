package com.github.tornaia.jimglabel.common.clock;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ClockServiceDefaultImpl implements ClockService {

    @Override
    public long now() {
        return System.currentTimeMillis();
    }

    @Override
    public Date date() {
        return new Date(now());
    }
}
