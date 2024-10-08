package com.oneinstep.jupiter.threadpool.web;

import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import com.oneinstep.jupiter.threadpool.config.ThreadPoolConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/thread-pool/api/")
public class ThreadPoolRestController {

    @Resource
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    /**
     * 线程池列表
     *
     * @return index
     */
    @GetMapping({"pools", "pools/"})
    public ApiResult<List<ThreadPoolConfig>> pools() {
        return ApiResult.success(dynamicThreadPoolManager.getAllPoolConfig());
    }

    /**
     * 线程池详情
     *
     * @param poolName 线程池名称
     * @return EDIT
     */
    @GetMapping("{poolName}")
    public ApiResult<ThreadPoolConfig> pool(@PathVariable @Nonnull String poolName) {
        return ApiResult.success(dynamicThreadPoolManager.getPoolConfig(poolName));
    }

    /**
     * 修改线程池
     *
     * @param request 请求参数
     * @return index
     */
    @PostMapping("{poolName}")
    public ApiResult<Void> modifyThreadPool(@PathVariable @Nonnull String poolName, @RequestBody ThreadPoolConfig request) {
        try {
            dynamicThreadPoolManager.modifyThreadPool(request);
        }
        // 成功则重定向到首页，失败留在当前页面，并显示错误信息
        catch (Exception e) {
            log.error("修改线程池失败", e);
            return ApiResult.fail(e.getMessage());
        }

        return ApiResult.success();
    }

    /**
     * 重置线程池
     *
     * @param poolName 线程池名称
     * @return index
     */
    @PostMapping("{poolName}/reset")
    public ApiResult<Void> resetThreadPool(@PathVariable @Nonnull String poolName) {
        try {
            dynamicThreadPoolManager.resetThreadPool(poolName);
        } catch (Exception e) {
            log.error("重置线程池失败", e);
            return ApiResult.fail(e.getMessage());
        }
        return ApiResult.success();
    }

    @PostMapping("{poolName}/switchMonitor")
    public ApiResult<Void> switchMonitor(@PathVariable @Nonnull String poolName, boolean enable) {
        try {
            dynamicThreadPoolManager.switchMonitor(new SwitchMonitorParam(poolName, enable));
        } catch (Exception e) {
            log.error("切换监控状态失败", e);
            return ApiResult.fail(e.getMessage());
        }
        return ApiResult.success();
    }

    @PostMapping("/{poolName}/switchAdaptive")
    public ApiResult<Void> switchAdaptive(@PathVariable @Nonnull String poolName, boolean enable) {
        try {
            dynamicThreadPoolManager.switchAdaptive(new SwitchAdaptiveParam(poolName, enable));
        } catch (Exception e) {
            log.error("切换自适应状态失败", e);
            return ApiResult.fail(e.getMessage());
        }
        return ApiResult.success();
    }

}
