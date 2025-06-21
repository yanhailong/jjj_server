package com.jjg.game.common.utils;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * 随机工具类
 * @scene 1.0
 */
public class RandomUtils {

	private static final LongAdder counter = new LongAdder();
	private static final long BASE_TIMESTAMP = 1672531200000L;

	public static String getRandomString(int length) {
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; ++i) {
			int number = random.nextInt(62);// [0,62)
			sb.append(str.charAt(number));
		}
		return sb.toString();
	}

	public static String getUUid(){
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	public static int randomNum(int start,int end){
		return ThreadLocalRandom.current().nextInt(start, end);
	}

	/**
	 * 随机一个范围为[min,max]的数，包含min和max
	 *
	 * @param min
	 * @param max
	 */
	public static int randomMinMax(int min, int max){
		return ThreadLocalRandom.current().nextInt(min,max+1);
	}

	/**
	 * 随机一个范围为[min,max]的数，包含min和max
	 *
	 * @param min
	 * @param max
	 */
	public static double randomDoubleMinMax(double min, double max){
		return ThreadLocalRandom.current().nextDouble(min,max);
	}

	/**
	 * 随机一个范围为[min,max]的数，包含min和max
	 *
	 * @param min
	 * @param max
	 */
	public static long randomLongMinMax(long min, long max){
		return ThreadLocalRandom.current().nextLong(min,max+1);
	}

	/**
	 * 生成随机int
	 *
	 * @param max （不包含max）
	 */
	public static int randomInt(int max) {
		if (max <= 0) {
			throw new RuntimeException("max must > 0");
		}
		return ThreadLocalRandom.current().nextInt(max);
	}

	public static String generateId() {
		long time = (System.currentTimeMillis() - BASE_TIMESTAMP) << 22;
		long seq = counter.sum() & 0x3FFFFF;
		counter.increment();
		return Long.toHexString(time | seq);
	}

}
