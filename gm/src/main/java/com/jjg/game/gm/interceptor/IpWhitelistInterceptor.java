package com.jjg.game.gm.interceptor;


import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.gm.exception.AccessDeniedException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;



import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IP白名单拦截器 - 第一层安全防护
 * 优先级最高，用于过滤非法IP访问
 */
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor, Ordered {

    private final Logger log = LoggerFactory.getLogger(IpWhitelistInterceptor.class);

    @Autowired
    private NodeConfig nodeConfig;
    /**
     * 从配置读取白名单IP列表
     * 格式: ["192.168.3.31", "10.0.0.0/24", "::1"]
     */
    private String[] whiteIpList;

//    /**
//     * 是否启用IP白名单
//     */
//    @Value("${cluster.ipWhitelist.enabled:true}")
//    private boolean enabled;

//    /**
//     * 排除的路径模式（不需要IP校验的路径）
//     */
//    @Value("#{'${cluster.ipWhitelist.excludePatterns:/health,/public/**,/api-docs/**,/swagger-ui/**,/favicon.ico}'.split(',')}")
//    private String[] excludePatterns;

    /**
     * 默认的白名单IP（本地访问）
     */
    private static final Set<String> DEFAULT_WHITELIST = new HashSet<>(Arrays.asList(
            "127.0.0.1",        // IPv4本地
            "0:0:0:0:0:0:0:1",  // IPv6本地
            "::1",              // IPv6本地简写
            "localhost"         // 本地主机名
    ));

    private final Set<String> whitelistSet = new HashSet<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        // 添加默认白名单
        whitelistSet.addAll(DEFAULT_WHITELIST);
        whiteIpList = nodeConfig.getWhiteIpList();
        // 添加配置的白名单
        if (whiteIpList != null) {
            for (String ip : whiteIpList) {
                if (ip != null && !ip.trim().isEmpty()) {
                    whitelistSet.add(ip.trim());
                }
            }
        }

//        log.info("IP白名单拦截器初始化完成，启用状态: {}", enabled);
        log.info("白名单IP列表: {}", whitelistSet);
//        log.info("排除路径模式: {}", Arrays.toString(excludePatterns));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // 如果不启用，直接放行
//        if (!enabled) {
//            return true;
//        }

        // 检查请求路径是否在排除列表中
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 移除contextPath部分
        if (contextPath != null && !contextPath.isEmpty()
                && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

//        // 检查排除路径
//        for (String pattern : excludePatterns) {
//            if (pathMatcher.match(pattern.trim(), requestUri)) {
//                log.debug("路径 {} 在排除列表中，跳过IP检查", requestUri);
//                return true;
//            }
//        }

        // 获取客户端真实IP
        String clientIp = getRealClientIp(request);

        // 检查IP是否在白名单中
        if (!isIpAllowed(clientIp)) {
            log.warn("IP白名单拒绝访问 - IP: {}, URI: {}, User-Agent: {}",
                    clientIp, requestUri, request.getHeader("User-Agent"));

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\": 403, \"message\": \"Access Denied. IP %s is not in whitelist.\"}",
                    clientIp
            ));

            throw new AccessDeniedException("IP " + clientIp + " 不在白名单中");
        }

        log.debug("IP白名单验证通过 - IP: {}, URI: {}", clientIp, requestUri);
        return true;
    }

    /**
     * 获取真实的客户端IP地址
     * 考虑了代理、负载均衡等场景
     */
    private String getRealClientIp(HttpServletRequest request) {
        // 常见的代理服务器IP头，按优先级排序
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "X-Real-IP"
        };

        String ip = null;

        // 按优先级检查代理头
        for (String header : ipHeaders) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        // 如果没有获取到有效IP，使用remoteAddr
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（如X-Forwarded-For: client, proxy1, proxy2）
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String i : ips) {
                String trimmedIp = i.trim();
                if (isValidIp(trimmedIp) && !isInternalIp(trimmedIp)) {
                    ip = trimmedIp;
                    break;
                }
            }
        }

        // 标准化本地IP
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "::1";
        }

        return ip == null ? "unknown" : ip;
    }

    /**
     * 验证IP地址是否有效
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 判断是否为内部IP
     */
    private boolean isInternalIp(String ip) {
        return ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.") ||
                ip.equals("127.0.0.1") ||
                ip.equals("::1");
    }

    /**
     * 检查IP是否在白名单中
     * 支持格式：
     * 1. 单个IP: 192.168.3.31
     * 2. IP段CIDR: 192.168.3.0/24
     * 3. 通配符: 192.168.3.*
     */
    private boolean isIpAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // 精确匹配
        if (whitelistSet.contains(ip)) {
            return true;
        }

        // 检查IPv6本地地址映射
        if (ip.equals("::1") && whitelistSet.contains("127.0.0.1")) {
            return true;
        }

        // 检查每个白名单规则
        for (String whiteIp : whitelistSet) {
            if (matchesIpRule(ip, whiteIp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 匹配IP规则
     */
    private boolean matchesIpRule(String ip, String rule) {
        // 精确匹配
        if (rule.equals(ip)) {
            return true;
        }

        // CIDR格式匹配
        if (rule.contains("/")) {
            return isIpInCidr(ip, rule);
        }

        // 通配符匹配
        if (rule.contains("*")) {
            return matchesWildcard(ip, rule);
        }

        return false;
    }

    /**
     * CIDR格式匹配
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            // 使用commons-net的SubnetUtils（需要添加依赖）
            // 这里简单实现IPv4 CIDR匹配，实际项目中建议使用完整实现
            if (cidr.contains("/")) {
                String[] parts = cidr.split("/");
                String network = parts[0];
                int prefix;
                try {
                    prefix = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return false;
                }

                // 简单实现，实际项目建议使用专业库
                return isIpv4InRange(ip, network, prefix);
            }
        } catch (Exception e) {
            log.warn("CIDR匹配失败: ip={}, cidr={}", ip, cidr, e);
        }
        return false;
    }

    /**
     * 简单的IPv4 CIDR匹配
     */
    private boolean isIpv4InRange(String ip, String network, int prefix) {
        try {
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(network);
            long mask = (prefix == 32) ? 0xffffffffL : (0xffffffffL << (32 - prefix));

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * IP地址转long
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= Long.parseLong(octets[i]) << (24 - (8 * i));
        }
        return result;
    }

    /**
     * 通配符匹配
     */
    private boolean matchesWildcard(String ip, String pattern) {
        // 将通配符转换为正则表达式
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return ip.matches(regex);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // 后处理，可以添加日志等
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求完成后的处理
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 最高优先级
    }
}
