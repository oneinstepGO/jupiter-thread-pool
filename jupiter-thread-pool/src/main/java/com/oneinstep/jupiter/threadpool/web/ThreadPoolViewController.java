package com.oneinstep.jupiter.threadpool.web;

import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import com.oneinstep.jupiter.threadpool.config.ThreadPoolConfig;
import com.oneinstep.jupiter.threadpool.support.NoSuchNamedThreadPoolException;
import com.oneinstep.jupiter.threadpool.support.SwitchAdaptiveParam;
import com.oneinstep.jupiter.threadpool.support.SwitchMonitorParam;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@Slf4j
public class ThreadPoolViewController {

    @Resource
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    private static final String REDIRECT = "redirect:/thread-pool";
    private static final String EDIT = "thread-pool/edit";
    private static final String INDEX = "thread-pool/index";
    private static final String THREAD_POOL = "threadPool";
    private static final String THREAD_POOLS = "threadPools";
    private static final String ERROR = "error";

    /**
     * 线程池列表
     *
     * @param model model
     * @return index
     */
    @GetMapping({"/thread-pool", "/thread-pool/"})
    public String index(Model model) {
        model.addAttribute(THREAD_POOLS, dynamicThreadPoolManager.getAllPoolConfig());
        return INDEX;
    }

    /**
     * 线程池详情
     *
     * @param poolName 线程池名称
     * @param model    model
     * @return EDIT
     */
    @GetMapping("/thread-pool/{poolName}")
    public String editThreadPool(@PathVariable @Nonnull String poolName, Model model) {
        model.addAttribute(THREAD_POOL, dynamicThreadPoolManager.getPoolConfig(poolName));
        return EDIT;
    }

    /**
     * 修改线程池
     *
     * @param request 请求参数
     * @param model   model
     * @return index
     */
    @PostMapping("/thread-pool/modify")
    public String modifyThreadPool(ThreadPoolConfig request, Model model) {
        try {
            dynamicThreadPoolManager.modifyThreadPool(request);
        }
        // 成功则重定向到首页，失败留在当前页面，并显示错误信息
        catch (NoSuchNamedThreadPoolException e) {
            log.error("修改线程池失败", e);
            model.addAttribute(ERROR, "线程池不存在");
            model.addAttribute(THREAD_POOL, dynamicThreadPoolManager.getPoolConfig(request.getPoolName()));
            return EDIT;

        } catch (Exception e) {
            log.error("修改线程池失败", e);
            model.addAttribute(ERROR, e.getMessage());
            model.addAttribute(THREAD_POOL, dynamicThreadPoolManager.getPoolConfig(request.getPoolName()));
            return EDIT;
        }

        return REDIRECT;
    }

    /**
     * 重置线程池
     *
     * @param poolName 线程池名称
     * @param model    model
     * @return index
     */
    @PostMapping("/thread-pool/reset")
    public String resetThreadPool(@Nonnull String poolName, Model model) {
        try {
            dynamicThreadPoolManager.resetThreadPool(poolName);
        } catch (Exception e) {
            log.error("重置线程池失败", e);
            model.addAttribute(ERROR, e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/thread-pool/switchMonitor")
    public String switchMonitor(@RequestBody SwitchMonitorParam param, Model model) {
        try {
            dynamicThreadPoolManager.switchMonitor(param);
        } catch (Exception e) {
            log.error("切换监控状态失败", e);
            model.addAttribute(ERROR, e.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/thread-pool/switchAdaptive")
    public String switchAdaptive(@RequestBody SwitchAdaptiveParam param, Model model) {
        try {
            dynamicThreadPoolManager.switchAdaptive(param);
        } catch (NoSuchNamedThreadPoolException e) {
            log.error("切换自适应状态失败", e);
            model.addAttribute(ERROR, "线程池: " + param.poolName() + " 不存在");
        } catch (Exception e) {
            log.error("切换自适应状态失败", e);
            model.addAttribute(ERROR, e.getMessage());
        }
        return REDIRECT;
    }
}
