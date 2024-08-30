package com.oneinstep.jupiter.threadpool.support;

import jakarta.annotation.Nonnull;

public record SwitchAdaptiveParam(@Nonnull String poolName, boolean enableAdaptive) {
}
