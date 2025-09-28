package com.jjg.game.gm.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author 11
 * @date 2025/9/26 11:27
 */
public class NetUtil {
    public static boolean isValidIP(String ip) {
        try {
            return InetAddress.getByName(ip).getHostAddress().equals(ip);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
