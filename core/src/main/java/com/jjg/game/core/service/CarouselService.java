package com.jjg.game.core.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.data.Carousel;
import com.jjg.game.core.pb.gm.CarouselUpdateInfo;
import com.jjg.game.core.pb.gm.NotifyCarouselUpdate;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 轮播数据服务
 * 多节点环境下只有一个节点加载数据，其他节点共享数据
 */
@Service
public class CarouselService {

    private final Logger log = LoggerFactory.getLogger(CarouselService.class);

    private final String tableName = "carousel";

    private final RedisTemplate<String, Carousel> redisTemplate;


    private final ClusterSystem clusterSystem;

    public CarouselService(RedisTemplate<String, Carousel> redisTemplate, ClusterSystem clusterSystem) {
        this.redisTemplate = redisTemplate;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 获取轮播数据列表
     */
    public List<Carousel> getCarouselList() {
        return redisTemplate.opsForHash().values(tableName).stream().map(o -> (Carousel) o).toList();
    }

    /**
     * 根据ID获取轮播数据
     */
    public Carousel getCarouselById(long id) {
        return (Carousel) redisTemplate.opsForHash().get(tableName, id);
    }

    /**
     * 根据ID覆盖更新轮播数据
     */
    public void updateCarousel(Carousel carousel) {
        if (carousel == null || carousel.getId() <= 0) {
            log.warn("更新轮播数据失败，数据为空或ID无效: {}", carousel);
            return;
        }
        try {
            redisTemplate.opsForHash().put(tableName, carousel.getId(), carousel);
            log.debug("更新轮播数据成功，ID: {}", carousel.getId());
        } catch (Exception e) {
            log.error("更新轮播数据失败，ID: {}", carousel.getId(), e);
        }
    }

    /**
     * 根据ID删除轮播数据
     */
    public boolean deleteCarouselById(long id) {
        if (id <= 0) {
            log.warn("删除轮播数据失败，ID无效: {}", id);
            return false;
        }
        try {
            Long result = redisTemplate.opsForHash().delete(tableName, id);
            boolean success = result > 0;
            if (success) {
                log.debug("删除轮播数据成功，ID: {}", id);
            } else {
                log.warn("删除轮播数据失败，数据不存在，ID: {}", id);
            }
            return success;
        } catch (Exception e) {
            log.error("删除轮播数据异常，ID: {}", id, e);
        }
        return false;
    }

    /**
     * 覆盖所有轮播数据
     */
    public void coverrAllCarousel(Map<Long, Carousel> map) {
        try {
            redisTemplate.delete(tableName);
            if (map != null && !map.isEmpty()) {
                redisTemplate.opsForHash().putAll(tableName, map);
            }
            log.debug("覆盖所有轮播数据成功 map = {}", map == null ? null : JSON.toJSONString(map));
        } catch (Exception e) {
            log.error("清空所有轮播数据失败", e);
        }
    }

    /**
     * 通知大厅节点轮播数据变化
     */
    public void notifyHallCarouselUpdate(List<CarouselUpdateInfo> carouselUpdateInfo) {
        if (carouselUpdateInfo == null || carouselUpdateInfo.isEmpty()) {
            return;
        }
        NotifyCarouselUpdate notifyCarouselUpdate = new NotifyCarouselUpdate();
        notifyCarouselUpdate.getCarousel().addAll(carouselUpdateInfo);
        PFMessage pfMessage = MessageUtil.getPFMessage(notifyCarouselUpdate);
        clusterSystem.notifyNode(pfMessage, Set.of(NodeType.HALL.toString())::contains);
    }

    public String getMd5(Carousel carousel) {
        String jsonString = JSON.toJSONString(carousel);
        return DigestUtils.md5Hex(jsonString);
    }

    /**
     * 同步轮播数据
     * newCarouselList 为空：表示全部删除
     */
    public void sync(List<Carousel> newCarouselList) {
        try {
            // redis 中旧的配置
            List<Carousel> oldSourceList = getCarouselList();

            // 新配置为空，表示全删
            if (newCarouselList == null || newCarouselList.isEmpty()) {
                if (oldSourceList == null || oldSourceList.isEmpty()) {
                    log.warn("轮播数据同步：新旧数据均为空，无需处理");
                    return;
                }

                List<CarouselUpdateInfo> syncList = new ArrayList<>();
                for (Carousel oldItem : oldSourceList) {
                    syncList.add(new CarouselUpdateInfo(CarouselUpdateInfo.CarouselUpdateType.DELETE, new Carousel(oldItem.getId())));
                }

                // 覆盖到redis
                coverrAllCarousel(null);
                // 通知大厅节点
                notifyHallCarouselUpdate(syncList);
                log.info("轮播数据同步完毕！全部删除，共 [{}] 条", syncList.size());
                return;
            }

            // 新配置不为空
            Map<Long, Carousel> oldSourceMap = oldSourceList.stream().collect(Collectors.toMap(Carousel::getId, Function.identity(), (a, b) -> a));

            Map<Long, Carousel> newMap = newCarouselList.stream().collect(Collectors.toMap(Carousel::getId, Function.identity(), (a, b) -> b));

            List<CarouselUpdateInfo> syncList = new ArrayList<>();
            int add = 0, update = 0, delete = 0;

            // 1. 新增 & 更新
            for (Carousel newItem : newCarouselList) {
                Carousel oldItem = oldSourceMap.get(newItem.getId());
                if (oldItem == null) {
                    // 新增
                    syncList.add(new CarouselUpdateInfo(CarouselUpdateInfo.CarouselUpdateType.UPDATE, newItem));
                    add++;
                } else if (!getMd5(newItem).equals(getMd5(oldItem))) {
                    // 更新
                    syncList.add(new CarouselUpdateInfo(CarouselUpdateInfo.CarouselUpdateType.UPDATE, newItem));
                    update++;
                }
            }

            // 2. 删除
            for (Carousel oldItem : oldSourceList) {
                if (!newMap.containsKey(oldItem.getId())) {
                    syncList.add(new CarouselUpdateInfo(CarouselUpdateInfo.CarouselUpdateType.DELETE, new Carousel(oldItem.getId())));
                    delete++;
                }
            }

            // 3. 更新缓存 & 通知
            if (!syncList.isEmpty()) {
                coverrAllCarousel(newMap);
                notifyHallCarouselUpdate(syncList);
            }
            log.info("轮播数据同步完毕！新增[{}]条，更新[{}]条，删除[{}]条！", add, update, delete);
        } catch (Exception e) {
            log.error("同步轮播数据错误！", e);
        }
    }


}
