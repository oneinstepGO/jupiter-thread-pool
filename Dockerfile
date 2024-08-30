# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /home/app

# 将整个 jupiter-thread-pool 目录复制到 /home/app/jupiter-thread-pool
COPY jupiter-thread-pool /home/app/jupiter-thread-pool

# 将整个 thread-monitor-demo 目录复制到 /home/app/thread-monitor-demo
COPY thread-monitor-demo /home/app/thread-monitor-demo

# 进入 jupiter-thread-pool 用 maven 进行 install
RUN mvn -f /home/app/jupiter-thread-pool/pom.xml clean install -DskipTests -e

# 进入 thread-monitor-demo 用 maven 进行 package
RUN mvn -f /home/app/thread-monitor-demo/pom.xml clean package -DskipTests -e

# Stage 2: Create the common image
FROM openjdk:17-jdk-slim
WORKDIR /usr/local/lib

# 复制打包后的 JAR 文件到公共镜像中
COPY --from=build /home/app/thread-monitor-demo/target/thread-monitor-demo-0.0.1-SNAPSHOT.jar ./thread-monitor-demo.jar

# 暴露端口
EXPOSE 8081
EXPOSE 18081

# 设置容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "/usr/local/lib/thread-monitor-demo.jar"]
