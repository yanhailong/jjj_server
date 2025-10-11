package com.jjg.game.common.utils;

import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * 随机工具类
 *
 * @author nobody
 * @scene 1.0
 */
public class RandomUtils {
    private static double EPSILON = 0.000001;

    public static Random RND = new Random(System.currentTimeMillis());

    private static LongAdder counter = new LongAdder();
    private static long BASE_TIMESTAMP = 1672531200000L;

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            // [0,62)
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String getUUid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static int randomNum(int start, int end) {
        return ThreadLocalRandom.current().nextInt(start, end);
    }

    /**
     * 随机一个范围为[min,max]的数，包含min和max
     *
     * @param min
     * @param max
     */
    public static int randomMinMax(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 随机一个范围为[min,max]的数，包含min和max
     *
     * @param min
     * @param max
     */
    public static double randomDoubleMinMax(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 随机一个范围为[min,max]的数，包含min和max
     *
     * @param min
     * @param max
     */
    public static long randomLongMinMax(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
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

    public static boolean nextBool() {
        return RND.nextBoolean();
    }

    public static boolean nextBool(int max, int f) {
        int v = getRandomNumIntMax(max);
        return (v < f);
    }

    public static <T> T randCollection(Collection<T> objs) {
        int size = objs.size();
        if (size < 1) {
            return null;
        } else if (size == 1) {
            return (T) objs.toArray()[0];
        }

        int v = RND.nextInt(size - 1);
        return (T) objs.toArray()[v];
    }

    /**
     * n个里面选m个.函数的时间复杂度为O(m):和选择的数量成正比.<br>
     * 注意:此函数会改变传入List的元素顺序,如果你需要保持原List顺序不变,就拷贝一份List传入<br>
     * 此函数能够保证每个元素被选择的概率是均等的:<br>
     * 函数会对List中的元素进行划分,次数至多为m次;当函数结束,随机选择的m个元素就会放在List的最前面.<br>
     *
     * @param <T>
     * @return
     */
    public static <T> void nRandSelectM(List<T> objs, int m) {
        // 不能为空
        if (objs.isEmpty()) {
            throw new RuntimeException("n个选m个,List本身为空!");
        }
        // 至少选择一个
        if (m <= 0) {
            throw new RuntimeException("n个选m个,m必须为正数!m:" + m);
        }
        // 数量上限
        int n = objs.size();
        if (m > n) {
            throw new RuntimeException("n个选m个,m必须小于等于n!n:" + n + " m:" + m);
        }

        // 全部选择,就没必要进行随机划分,直接返回即可
        if (m == n) {
            return;
        }
        // 否则,进行m次随机划分
        for (int i = 0; i < m; i++) {
            // 选择一个随机位置(为了保证等概率,这里必须要包含当前位置) 取区间[i,n)中的某个值
            int randIndex = nextInt(i, n);
            // 交换位置
            if (i != randIndex) {
                T tmp = objs.get(i);
                objs.set(i, objs.get(randIndex));
                objs.set(randIndex, tmp);
            }
        }
        // 当循环结束时,m个随机的元素就放置在List的最前面了
    }

    /**
     * 包含0,不包含最大值
     *
     * @param max
     * @return
     */
    public static int nextInt(int max) {
        if (max < 0) {
            return 0;
        }
        return RND.nextInt(max);
    }

    /**
     * 包含最小值,不包含最大值
     *
     * @param f
     * @param t
     * @return
     */
    public static int nextInt(int f, int t) {
        if (t <= f) {
            return f;
        }
        return RND.nextInt(t - f) + f;
    }

    /**
     * 包含最小值和最大值
     *
     * @param f
     * @param t
     * @return
     */
    public static int nextIntInclude(int f, int t) {
        if (t + 1 <= f) {
            return f;
        }
        return RND.nextInt(t + 1 - f) + f;
    }


    /**
     * 包含最小值和最大值
     *
     * @param f
     * @param t
     * @return
     */
    public static long nextLongInclude(long f, long t) {
        if (t + 1 <= f) {
            return f;
        }
        return RND.nextLong(t + 1 - f) + f;
    }

    /**
     * 1-max之间的随机整数(包含)
     *
     * @return
     */
    public static int getRandomNumIntMax(int max) {
        return nextIntInclude(1, max);
    }

    /**
     * 1-100之间的随机整数(包含)
     *
     * @return
     */
    public static int getRandomNumInt100() {
        return nextIntInclude(1, 100);
    }

    /**
     * 1-10000之间的随机整数(包含)
     *
     * @return
     */
    public static int getRandomNumInt10000() {
        return nextIntInclude(1, 10000);
    }

    /**
     * 0-1之间的随机小数(包含0,包含1)
     *
     * @return
     */
    public static double getRandomNumDou() {
        double v = RND.nextFloat();
        return v;
    }

    /**
     * 0-1之间的随机小数,精度为小数点后两位(不包含0,包含1)
     *
     * @return
     */
    public static double getRandomNumDouble01() {
        return (double) nextIntInclude(1, 100) / 100;
    }

    /**
     * 取范围随机数：大于等于较小的数，小于较大的数
     */
    public static double randomValue(double v1, double v2) {
        if (Math.abs(v1 - v2) < EPSILON) {
            return v1;
        }
        RND.setSeed(System.nanoTime());
        if (v1 > v2) {
            return RND.nextDouble() * (v1 - v2) + v2;
        } else {
            return RND.nextDouble() * (v2 - v1) + v1;
        }
    }

    /**
     * f是否命中几率(百分比)
     *
     * @param f
     * @return
     */
    public static boolean getRandomBoolean100(int f) {
        return (getRandomNumInt100() <= f);
    }

    /**
     * 是否命中
     *
     * @param f
     * @return
     */
    public static boolean getRandomBoolean10000(int f) {
        return getRandomNumInt10000() <= f;
    }

    /**
     * 根据权重选出几个结果
     *
     * @param weightMap key是id，value是对应的权重
     * @param count     选的个数
     * @return
     */
    public static <T> Set<T> getRandomByWeight(Map<T, Integer> weightMap, int count) {
        return getRandomByWeight(weightMap, count, true);
    }

    /**
     * 根据权重选出几个结果
     *
     * @param weightMap   key是id，value是对应的权重
     * @param count       选的个数
     * @param checkZeroId 检测0值作为key时的随机退出规则
     * @return
     */
    public static <T> Set<T> getRandomByWeight(
            Map<T, Integer> weightMap, int count, boolean checkZeroId) {
        int totalWeight = 0;
        for (Integer val : weightMap.values()) {
            totalWeight += val;
        }
        return getRandomByWeight(totalWeight, weightMap, count, checkZeroId);
    }

    /**
     * 根据权重选出几个结果
     *
     * @param weight    总权重
     * @param weightMap key是id，value是对应的权重
     * @param count     选的个数
     * @return
     */
    public static Set<Integer> getRandomByWeight(
            int weight, Map<Integer, Integer> weightMap, int count) {
        return getRandomByWeight(weight, weightMap, count, true);
    }

    /**
     * 根据权重选出几个结果
     *
     * @param weight      总权重
     * @param weightMap   key是id，value是对应的权重
     * @param count       选的个数
     * @param checkZeroId 检测0值作为key时的随机退出规则
     * @return
     */
    public static <T> Set<T> getRandomByWeight(
            int weight, Map<T, Integer> weightMap, int count, boolean checkZeroId) {
        Map<T, Integer> map = new HashMap<>(weightMap);
        Set<T> resultSet = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            if (weight == 0) {
                break;
            }
            // 随机一个
            int rand = new Random().nextInt(weight) + 1;
            int r = 0;
            T choose = null;
            for (T id : map.keySet()) {
                int w = map.get(id);
                r += w;
                if (rand <= r) {
                    choose = id;
                    break;
                }
            }
            if (!checkZeroId || choose != null) {
                // 删除本次随机到的id的权重
                weight -= map.get(choose);
                map.remove(choose);
                resultSet.add(choose);
            } else {
                break;
            }
        }
        return resultSet;
    }

    /**
     * 从权重列表中随机一个，并从上下限中随机一个配置格式 (权重:值下限+值上限;)
     */
    public static Integer randomMaxMinByWeightList(List<List<Integer>> weightList) {
        List<Integer> rangList = randCollection(weightList);
        if (rangList == null || rangList.size() < 2) {
            return null;
        }
        return nextIntInclude(rangList.get(0), rangList.get(1));
    }


    /**
     * 从权重列表中随机一个，并从上下限中随机一个配置格式 (权重:值下限+值上限;)
     */
    public static Long randomLongMaxMinByWeightList(List<List<Long>> weightList) {
        List<Long> rangList = randCollection(weightList);
        if (rangList == null || rangList.size() < 2) {
            return null;
        }
        return nextLongInclude(rangList.get(0), rangList.get(1));
    }

    /**
     * 从权重列表中随机一个
     */
    public static long randomWeightList(List<List<Long>> weightList) {
        WeightRandom<Integer> random = new WeightRandom<>();
        for (int i = 0; i < weightList.size(); i++) {
            random.add(i, weightList.get(i).getFirst());
        }
        Integer next = random.next();
        return RandomUtil.randomLong(weightList.get(next).get(1), weightList.get(next).getLast());
    }

    /**
     * 从权重列表中随机一个
     */
    public static Integer randomByWeightList(List<List<Integer>> weightList) {
        int totalWeight = 0, target = 0, tmpWeight = 0;
        for (List<Integer> weightConf : weightList) {
            totalWeight += weightConf.get(0);
        }
        int randomInt = randomInt(totalWeight);
        for (List<Integer> data : weightList) {
            tmpWeight += data.get(0);
            if (randomInt < tmpWeight) {
                target = data.get(1);
                break;
            }
        }
        return target;
    }

    /**
     * 随机获得列表中的元素
     *
     * @param <T>   元素类型
     * @param list  列表
     * @param limit 限制列表的前N项
     * @return 随机元素
     */
    public static <T> T randomEle(final List<T> list, int limit) {
        if (list.size() < limit) {
            limit = list.size();
        }
        return list.get(randomInt(limit));
    }

    /**
     * 随机获得列表中的一定量元素
     *
     * @param <T>   元素类型
     * @param list  列表
     * @param count 随机取出的个数
     * @return 随机元素
     */
    public static <T> List<T> randomEles(final List<T> list, final int count) {
        final List<T> result = new ArrayList<>(count);
        final int limit = list.size();
        while (result.size() < count) {
            result.add(randomEle(list, limit));
        }

        return result;
    }

    /**
     * 随机获得列表中的一定量的元素，返回List<br>
     * 此方法与{@link #randomEles(List, int)} 不同点在于，不会获取重复位置的元素
     *
     * @param source 列表
     * @param count  随机取出的个数
     * @param <T>    元素类型
     * @return 随机列表
     * @since 5.2.1
     */
    public static <T> List<T> randomEleList(final List<T> source, final int count) {
        if (count >= source.size()) {
            return new ArrayList<>(source);
        }
        final int[] randomList = PrimitiveArrayUtil.sub(randomInts(source.size()), 0, count);
        final List<T> result = new ArrayList<>();
        for (final int e : randomList) {
            result.add(source.get(e));
        }
        return result;
    }

    /**
     * 根据权重随机
     */
    public static <T> T randomByWeight(List<Pair<Integer, T>> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        if (source.size() == 1) {
            return source.getFirst().getSecond();
        }
        Integer totalWeight = 0;
        Map<Pair<Integer, T>, Integer> weightMap = new LinkedHashMap<>(source.size());
        for (Pair<Integer, T> weightPair : source) {
            totalWeight += weightPair.getFirst();
            weightMap.put(weightPair, totalWeight);
        }
        Set<Pair<Integer, T>> randomByWeight = getRandomByWeight(weightMap, 1);
        return randomByWeight.stream().map(Pair::getSecond).toList().getFirst();
    }

    /**
     * 创建指定长度的随机索引
     *
     * @param length 长度
     * @return 随机索引
     * @since 5.2.1
     */
    public static int[] randomInts(final int length) {
        final int[] range = PrimitiveArrayUtil.range(length);
        for (int i = 0; i < length; i++) {
            final int random = randomNum(i, length);
            PrimitiveArrayUtil.swap(range, i, random);
        }
        return range;
    }

    /**
     * 通过权重随机
     *
     * @param weightMap 权重map
     * @return 随机值
     */
    public static Integer randomByWeight(final Map<Integer, Integer> weightMap) {
        WeightRandom<Integer> random = new WeightRandom<>();
        for (Map.Entry<Integer, Integer> entry : weightMap.entrySet()) {
            random.add(entry.getKey(), entry.getValue());
        }
        return random.next();
    }

}
