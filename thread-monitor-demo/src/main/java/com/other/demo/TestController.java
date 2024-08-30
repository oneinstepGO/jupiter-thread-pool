package com.other.demo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {

    @Resource
    private TestCommandLineRunner runner;

    @GetMapping("/test/addPressure")
    public String addPressure(@Nonnull String taskName) {
        TestThread testThread = runner.getThreadMap().get(taskName);
        if (testThread == null) {
            log.error("taskName:{} not found", taskName);
            return "taskName not found";
        }
        testThread.addPressure();
        return "success";
    }

    @GetMapping("/test/reducePressure")
    public String reducePressure(@Nonnull String taskName) {
        TestThread testThread = runner.getThreadMap().get(taskName);
        if (testThread == null) {
            log.error("taskName:{} not found", taskName);
            return "taskName not found";
        }
        testThread.reducePressure();
        return "success";
    }

}
