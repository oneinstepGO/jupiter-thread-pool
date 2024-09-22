# jupiter-thread-pool

### 项目说明

本项目是一个基于 `Spring Boot` 的线程池监控组件，提供动态配置线程池参数的功能，支持监控线程池的运行状态，并实现线程池参数的自动调整。

#### 主要功能

- **动态配置线程池参数**：支持在运行时动态调整线程池的核心参数。
- **线程池运行状态监控**：通过 `Spring Boot` 的 `actuator` 模块，结合 `Prometheus` 暴露监控指标，并通过 `Grafana` 展示监控图表。
- **自定义指标统计**：基于滑动窗口算法，支持 `QPS`、`平均耗时`、`总数`、`异常数`、`成功率`等指标。
- **支持多种队列类型**：包括 `LinkedBlockingQueue`、`ArrayBlockingQueue`、`SynchronousQueue`、`PriorityBlockingQueue`。
- **支持多种拒绝策略**：包括 `AbortPolicy`、`CallerRunsPolicy`、`DiscardOldestPolicy`、`DiscardPolicy`。
- **全局配置选项**：可对所有线程池生效的全局配置。

#### 功能展示
![](https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1725035752510-83b95327-bc6f-4437-a6fd-1b867603c6ed.png)

![](https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1725043065094-87f62eaf-69a7-4d77-a009-46c8b013c4a3.png)


![机器全局监控](https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1725041761800-7d340971-2475-4470-a935-fcb05f2b1c94.png)

![线程池监控](https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1725049340538-4c21325d-76f3-4994-817a-6983b09598ec.png)

![任务监控](https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1725049423826-dab5c911-9934-47a0-9b35-7cc9f0cb4d84.png)

#### 全局配置参数

- `dynamic-thread-pool.monitor.export.enabled`：是否开启监控指标的暴露，默认 `true`。
- `dynamic-thread-pool.monitor.export.port`：暴露监控指标的端口，默认 `18081`。
- `dynamic-thread-pool.monitor.export.step`：监控指标的采集间隔，默认 `1s`。
- `dynamic-thread-pool.adaptive.enabled`：是否开启线程池参数自动调整，默认 `false`，需先开启对应线程池监控。
- `dynamic-thread-pool.monitor.baseMonitorUrl`：监控指标的基础 URL，用于拼接线程池监控 URL。
- `dynamic-thread-pool.adaptive.adjustmentIntervalMs`：自动调整线程池参数的间隔时间，默认 `30000ms`。

#### 使用限制

- **线程池管理界面**：只能查看和修改线程池的参数配置信息，不能新增或删除线程池，新增或删除线程池需要修改配置文件并重启服务。
- **本地配置**：只能查看和修改本机的线程池配置，无法查看和修改其他机器的线程池配置。使用者可以对接 RESTful API
  结合前端页面实现多机器的线程池管理。
- **Grafana 配置**：监控面板的 JSON
  配置文件位于 `grafana/provisioning/dashboards/thread-pool-monitor.json` 目录下，可以直接导入到 Grafana
  中使用，默认数据源名称为 `Prometheus-Thread-Monitor`，可全局替换为你的数据源名称。
- **生产使用**：如需在生产环境中使用，请务必先经过充分测试。

> 注意：如果覆盖了 `management.endpoints.web.exposure.include` 配置，需要确保包含了 `prometheus` 或 `all`，否则监控指标无法暴露。

### 使用方式

1. **引入依赖**

   将本项目中的 `jupiter-thread-pool` 模块打包成 `jar` 包，通过 Maven 或其它方式引入到你的项目中。

2. **添加配置**

   在你的 `Spring Boot` 项目的 `application.yml` 或 `application.properties` 中添加线程池初始化配置，例如：

   ```yaml
   dynamic-thread-pool:
     pools:
       bizThreadPool:
         corePoolSize: 10
         maxPoolSize: 10
         keepAliveTimeMs: 6000
         workQueue:
           type: "LinkedBlockingQueue"
           capacity: 32
         policy: "AbortPolicy"
         monitor:
           enabled: true
           timeWindowSeconds: 3
           monitorUrl: "xxx"
       # 可以添加多个线程池配置
       otherThreadPool:
         ...
     monitor:
       baseMonitorUrl: "xxx"
   ```

3. **启动项目**

   在启动类上添加 `@EnableDynamicThreadPool` 注解，启动你的项目。
   > 需要添加 JVM 参数  `--add-opens java.base/java.util.concurrent=ALL-UNNAMED`

4. **访问管理界面**

   访问 `http://{{your_server_host}}:{{your_server_port}}/thread-pool`，例如：`http://localhost:8081/thread-pool`
   即可查看线程池监控管理页面。如果使用了 `Spring Security` 或其他安全框架，需要配置对应的权限。

5. **查看监控指标**

   访问 `http://{{your_server_host}}:{{your_metrics_export_port}}/actuator/prometheus`
   ，例如：`http://localhost:18081/actuator/prometheus` 即可查看线程池的监控指标。

### Docker 一键启动演示项目

1. 安装 Docker。
2. 进入项目根目录 `mp-weixin-demo/thread-monitor`，执行以下命令：

   ```shell
   docker compose up -d
   ```

3. 访问 `http://localhost:8081/thread-pool` 查看线程池管理页面，点击线程池列表的监控按钮，查看对应的线程池监控图表。

通过上述步骤，你可以快速集成并使用本项目提供的线程池监控与管理功能。如果有任何问题或建议，欢迎反馈。

#### 相关阅读

- [还在为“线程池调优”而烦恼吗？](https://mp.weixin.qq.com/s/tqH9ywEZfsBPZChUUAmODg)
- [如何利用 Prometheus 和 Grafana 监控你的应用？](https://mp.weixin.qq.com/s/Icccgmv8MGM6Kl4duENedg)

---

欢迎关注我的公众号“**子安聊代码**”，一起探讨技术。 
<div style="text-align: center;">
    <img src="https://mp-img-1300842660.cos.ap-guangzhou.myqcloud.com/1724139391246-0dfa7dad-5977-44c1-90ca-55539184d575.jpg" style="width: 100px;" alt="">
</div>