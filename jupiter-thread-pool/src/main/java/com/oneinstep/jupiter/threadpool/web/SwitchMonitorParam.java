package com.oneinstep.jupiter.threadpool.web;

import jakarta.annotation.Nonnull;

/**
 * 开关监控参数
 *
 * @param poolName      线程池名称
 * @param enableMonitor 是否开启监控
 */
public record SwitchMonitorParam(@Nonnull String poolName,
                                 boolean enableMonitor) {
}
