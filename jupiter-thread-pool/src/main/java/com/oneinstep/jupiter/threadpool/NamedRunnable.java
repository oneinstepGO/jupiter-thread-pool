package com.oneinstep.jupiter.threadpool;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * A named task.
 * 执行固定任务
 */
@Getter
public class NamedRunnable implements Runnable {

    // 任务名称
    private final String name;
    // 任务
    private final Runnable runnable;

    @Setter
    private long startTime;

    @Setter
    private long submitTime;

    @Setter
    private long endTime;

    public NamedRunnable(String name, Runnable runnable) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (runnable == null) {
            throw new IllegalArgumentException("runnable cannot be null");
        }
        this.name = name;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }

}
