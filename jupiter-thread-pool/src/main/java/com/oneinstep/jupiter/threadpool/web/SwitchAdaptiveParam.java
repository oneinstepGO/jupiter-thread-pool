package com.oneinstep.jupiter.threadpool.web;

import jakarta.annotation.Nonnull;

public record SwitchAdaptiveParam(@Nonnull String poolName, boolean enableAdaptive) {
}
