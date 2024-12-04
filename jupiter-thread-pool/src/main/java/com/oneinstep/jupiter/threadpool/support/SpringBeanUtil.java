package com.oneinstep.jupiter.threadpool.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * spring bean 工具类
 */
@Component
public class SpringBeanUtil implements ApplicationContextAware {

    private static final SpringBeanUtil INSTANCE = new SpringBeanUtil();

    private ApplicationContext context;

    private SpringBeanUtil() {
    }

    /**
     * 根据类型获取spring 容器里面的bean
     *
     * @param clazz 类型class
     * @param <T>   类型
     * @return bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return INSTANCE.context.getBean(clazz);
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        return INSTANCE.context.getBean(beanName, clazz);
    }

    /**
     * 获取某个类的所有实例
     *
     * @param clazz 类型class
     * @param <T>   类型
     * @return bean
     */
    public static <T> Map<String, T> getBeanOfType(Class<T> clazz) {
        return INSTANCE.context.getBeansOfType(clazz);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        INSTANCE.context = applicationContext;
    }

}