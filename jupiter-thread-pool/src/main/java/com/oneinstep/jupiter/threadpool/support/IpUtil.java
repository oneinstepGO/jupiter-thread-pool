package com.oneinstep.jupiter.threadpool.support;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@UtilityClass
public class IpUtil {

    // 缓存单个IP地址
    private static String cachedIpAddress;

    // 缓存所有IP地址
    private static final Set<String> cachedIpAddresses = new HashSet<>();

    /**
     * 获取并缓存本地服务器的IP地址
     * @return 服务器的IP地址
     */
    public static String getServerIp() {
        if (cachedIpAddress == null) {
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                cachedIpAddress = inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                log.error("Failed to get server IP address", e);
                cachedIpAddress = "Unknown";
            }
        }
        return cachedIpAddress;
    }

    /**
     * 获取并缓存所有网络接口的IP地址
     * @return 所有IP地址的集合
     */
    public static Set<String> getAllServerIps() {
        if (cachedIpAddresses.isEmpty()) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        cachedIpAddresses.add(inetAddress.getHostAddress());
                    }
                }
            } catch (SocketException e) {
                log.error("Failed to get server IP addresses", e);
            }
        }
        return cachedIpAddresses;
    }

    /**
     * 清除缓存的IP地址
     */
    public static void clearCache() {
        cachedIpAddress = null;
        cachedIpAddresses.clear();
    }

}
