package com.jjg.game.core.dao;

import com.jjg.game.core.data.Marquee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/11 20:31
 */
@Repository
public class MarqueeDao {
    //跑马灯信息
    private final String marqueeTableName = "marquee";

    @Autowired
    private RedisTemplate redisTemplate;

    public List getAllMarquee() {
        return redisTemplate.opsForHash().values(marqueeTableName);
    }

    /**
     * 添加跑马灯
     * @param marquee
     */
    public void addMarquee(Marquee marquee) {
        redisTemplate.opsForHash().put(marqueeTableName, marquee.getId(), marquee);
    }

    /**
     * 添加跑马灯
     * @param marquee
     */
    public boolean addMarqueeIfAbsent(Marquee marquee) {
        return redisTemplate.opsForHash().putIfAbsent(marqueeTableName, marquee.getId(), marquee);
    }

    /**
     * 根据id获取跑马灯
     * @param id
     */
    public Marquee getMarquee(int id) {
        return (Marquee)redisTemplate.opsForHash().get(marqueeTableName,id);
    }

    public void removeMarquee(int id) {
        redisTemplate.opsForHash().delete(marqueeTableName,id);
    }

    public void removeMarquees(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().delete(marqueeTableName, ids.toArray());
    }

    public boolean exist(int id) {
        return redisTemplate.opsForHash().hasKey(marqueeTableName,id);
    }

    /**
     * 获取一个跑马灯
     * @return
     */
//    public Marquee getMarqueeByPriority() {
//        return getMarqueeByPriority(0);
//    }

//    private Marquee getMarqueeByPriority(int count) {
//        if(count >= 10){
//            return null;
//        }
//        //获取权重最高的一个
//        Set set = redisTemplate.opsForZSet().reverseRange(marqueePriorityTableName, 0, 0);
//        if(set == null || set.isEmpty()){
//            return null;
//        }
//        Object value = set.stream().findFirst().get();
//        if(value == null){
//            return null;
//        }
//
//        //获取跑马灯信息
//        long marqueeId = Long.parseLong(value.toString());
//        Object o = redisTemplate.opsForHash().get(marqueeTableName, marqueeId);
//        if(o == null){
//            redisTemplate.opsForZSet().popMax(marqueePriorityTableName);
//            return getMarqueeByPriority(count+1);
//        }
//
//        //检查次数是否
//        Marquee marquee = (Marquee) o;
//        if(marquee.getNums() < 1){
//            redisTemplate.opsForZSet().popMax(marqueePriorityTableName);
//            redisTemplate.opsForHash().delete(marqueeTableName, marqueeId);
//            return getMarqueeByPriority(count+1);
//        }
//
//        int now = TimeHelper.nowInt();
//        if(now > marquee.getEndTime()){
//            redisTemplate.opsForZSet().popMax(marqueePriorityTableName);
//            redisTemplate.opsForHash().delete(marqueeTableName, marqueeId);
//            return getMarqueeByPriority(count+1);
//        }
//        marquee.setNums(marquee.getNums() - 1);
//        redisTemplate.opsForHash().put(marqueeTableName, marquee.getId(), marquee);
//        return marquee;
//    }
}
