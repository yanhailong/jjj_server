package com.jjg.game.common.cluster;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * @since 1.0
 */
public class ClusterHelper {
    public static boolean inIpWhiteList(String ip, String[] whiteList) {
        if (whiteList == null || whiteList.length == 0) {
            return true;
        }
        return Arrays.asList(whiteList).contains(ip);
    }

    public static boolean preciseInIpWhiteList(String ip, String[] whiteList) {
        if (whiteList == null || whiteList.length == 0 || StringUtils.isEmpty(ip)) {
            return false;
        }
        return Arrays.asList(whiteList).contains(ip);
    }

    public static boolean preciseInIdWhiteList(long playerId, String[] whiteList) {
        if (whiteList == null || whiteList.length == 0 || playerId < 1) {
            return false;
        }
        for (String wId : whiteList) {
            if (Long.parseLong(wId) == playerId) {
                return true;
            }
        }
        return false;
    }

    public static boolean preciseInFlagsList(String[] flag1, String[] flags2) {
        if (flag1 == null || flag1.length == 0 || flags2 == null || flags2.length == 0) {
            return false;
        }

        for(String str : flag1) {
            for(String str2 : flags2) {
                if(str.equals(str2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
