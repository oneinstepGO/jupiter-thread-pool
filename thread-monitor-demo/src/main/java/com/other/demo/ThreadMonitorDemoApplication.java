package com.other.demo;

import com.oneinstep.jupiter.threadpool.EnableDynamicThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableDynamicThreadPool
@Slf4j
public class ThreadMonitorDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreadMonitorDemoApplication.class, args);
    }

}
