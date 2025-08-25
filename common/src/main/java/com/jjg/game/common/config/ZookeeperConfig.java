package com.jjg.game.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * zk 配置信息
 *
 * @scene 1.0
 *
 */
@Configuration
@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperConfig {
	/**
	 * 连接字符串
	 */
	private String connects;
	/**
	 * 连接间隔时间
	 */
	private int baseSleepTimeMs = 1000;
	/**
	 * 最大重试次数
	 */
	private int maxRetries = 10;

	/**
	 * 根目录
	 */
	private String marsRoot;

	/**
	 * zk session 超时时间
	 */
	private int sessionTimeoutMs = 10000;

	/**
	 * zk 连接 超时时间
	 */
	private int connectionTimeoutMs = 3000;

	public String getConnects() {
		return connects;
	}

	public void setConnects(String connects) {
		this.connects = connects;
	}

	public String getMarsRoot() {
		return marsRoot;
	}

	public void setMarsRoot(String marsRoot) {
		this.marsRoot = marsRoot;
	}

	public int getBaseSleepTimeMs() {
		return baseSleepTimeMs;
	}

	public void setBaseSleepTimeMs(int baseSleepTimeMs) {
		this.baseSleepTimeMs = baseSleepTimeMs;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getSessionTimeoutMs() {
		return sessionTimeoutMs;
	}

	public void setSessionTimeoutMs(int sessionTimeoutMs) {
		this.sessionTimeoutMs = sessionTimeoutMs;
	}

	public int getConnectionTimeoutMs() {
		return connectionTimeoutMs;
	}

	public void setConnectionTimeoutMs(int connectionTimeoutMs) {
		this.connectionTimeoutMs = connectionTimeoutMs;
	}
}
