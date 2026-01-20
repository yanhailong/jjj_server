package com.jjg.game.account.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.net.InetAddress;

/**
 * @author 11
 * @date 2026/1/20
 */
public class IpCountryUtil {
    private static DatabaseReader reader;

    static {
        try {
            File dbFile = new File("config/GeoLite2-Country.mmdb");
            reader = new DatabaseReader.Builder(dbFile).build();
        } catch (Exception e) {
            throw new RuntimeException("初始化IP库失败", e);
        }
    }

    public static String getCountryCode(String ip) {
        try {
            if(reader == null) {
                return null;
            }
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = reader.country(ipAddress);
            return response.getCountry().getIsoCode(); // CN / JP / US
        } catch (Exception e) {
            return null;
        }
    }
}
