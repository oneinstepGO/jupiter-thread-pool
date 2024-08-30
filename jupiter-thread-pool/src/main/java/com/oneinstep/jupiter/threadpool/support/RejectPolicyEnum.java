package com.oneinstep.jupiter.threadpool.support;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 拒绝策略枚举
 */
public enum RejectPolicyEnum {

    CALLER_RUNS_POLICY("CallerRunsPolicy", ThreadPoolExecutor.CallerRunsPolicy.class, ThreadPoolExecutor.CallerRunsPolicy::new),
    ABORT_POLICY("AbortPolicy", ThreadPoolExecutor.AbortPolicy.class, ThreadPoolExecutor.AbortPolicy::new),
    DISCARD_POLICY("DiscardPolicy", ThreadPoolExecutor.DiscardPolicy.class, ThreadPoolExecutor.DiscardPolicy::new),
    DISCARD_OLDEST_POLICY("DiscardOldestPolicy", ThreadPoolExecutor.DiscardOldestPolicy.class, ThreadPoolExecutor.DiscardOldestPolicy::new);

    private final String policyName;

    private final Class<?> policyClass;

    private final Supplier<RejectedExecutionHandler> policyFactory;

    RejectPolicyEnum(String policyName, Class<?> policyClass, Supplier<RejectedExecutionHandler> policyFactory) {
        this.policyName = policyName;
        this.policyClass = policyClass;
        this.policyFactory = policyFactory;
    }

    public RejectedExecutionHandler createPolicy() {
        return policyFactory.get();
    }

    public static RejectPolicyEnum getPolicyByName(String policyName) {
        return Stream.of(RejectPolicyEnum.values()).filter(policy -> policy.policyName.equals(policyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported rejection policy: " + policyName));
    }

    public static RejectPolicyEnum getPolicyByClass(Class<? extends RejectedExecutionHandler> policyClass) {
        return Stream.of(RejectPolicyEnum.values()).filter(policy -> policy.policyClass.equals(policyClass))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported rejection policy class: " + policyClass));
    }

    public String policyName() {
        return policyName;
    }

    public static RejectedExecutionHandler createRejectedExecutionHandler(String policy) {
        RejectPolicyEnum policyByName = RejectPolicyEnum.getPolicyByName(policy);
        return policyByName.createPolicy();
    }

}
