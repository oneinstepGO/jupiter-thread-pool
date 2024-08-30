/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oneinstep.jupiter.threadpool.metrics;

import lombok.Getter;
import lombok.Setter;

/**
 * The pane represents a window over a period of time.
 * <p>copy from dubbo-metrics</p>
 *
 * @param <T> The type of value the pane statistics.
 */
@Getter
public class Pane<T> {

    /**
     * Time interval of the pane in milliseconds.
     * -- GETTER --
     * Get the interval of the pane in milliseconds.
     */
    private final long intervalInMs;

    /**
     * Start timestamp of the pane in milliseconds.
     * -- GETTER --
     * Get start timestamp of the pane in milliseconds.
     */
    private volatile long startInMs;

    /**
     * End timestamp of the pane in milliseconds.
     * <p>
     * endInMs = startInMs + intervalInMs
     * -- GETTER --
     * Get end timestamp of the pane in milliseconds.
     */
    private volatile long endInMs;

    /**
     * Pane statistics value.
     * -- GETTER --
     * Get the pane statistics value.
     * <p>
     * <p>
     * -- SETTER --
     * Set new value to the pane, for reset the instance.
     */
    @Setter
    private T value;

    /**
     * @param intervalInMs interval of the pane in milliseconds.
     * @param startInMs    start timestamp of the pane in milliseconds.
     * @param value        the pane value.
     */
    public Pane(long intervalInMs, long startInMs, T value) {
        this.intervalInMs = intervalInMs;
        this.startInMs = startInMs;
        this.endInMs = this.startInMs + this.intervalInMs;
        this.value = value;
    }

    /**
     * Set the new start timestamp to the pane, for reset the instance.
     *
     * @param newStartInMs the new start timestamp.
     */
    public void setStartInMs(long newStartInMs) {
        this.startInMs = newStartInMs;
        this.endInMs = this.startInMs + this.intervalInMs;
    }

    /**
     * Reset the pane with new start timestamp and value.
     *
     * @param newStartInMs the new start timestamp.
     * @param newValue     the new value.
     */
    public void resetTo(long newStartInMs, T newValue) {
        setStartInMs(newStartInMs);
        setValue(newValue);
    }
}
