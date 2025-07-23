package com.jjg.game.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络工具
 *
 * @author 2CL
 */
public class NetUtils {

    /**
     * 获取当前服务器的外网/内网 IP（非回环、非本地地址）
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                // 跳过回环接口和未启用接口
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress()
                        && !addr.isLinkLocalAddress()
                        && addr instanceof java.net.Inet4Address) {
                        // 返回第一个非本地 IPv4 地址,如果本地开发时，如果开了虚拟机，可能会有多个虚拟ip地址
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("获取本地IP地址失败", e);
        }
        // 默认回退
        return "127.0.0.1";
    }
}
