package com.jjg.game.core.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.redis.RedisLock;
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

    /**
     * 数据同步完成的标记key
     */
    private final String syncCompleteKey = "carousel:sync:complete";

    private final RedisTemplate<String, Carousel> redisTemplate;

    /**
     * redis分布式锁
     */
    private final RedisLock redisLock;

    private final ClusterSystem clusterSystem;

    public CarouselService(RedisTemplate<String, Carousel> redisTemplate, RedisLock redisLock, ClusterSystem clusterSystem) {
        this.redisTemplate = redisTemplate;
        this.redisLock = redisLock;
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
    public void deleteCarouselById(long id) {
        if (id <= 0) {
            log.warn("删除轮播数据失败，ID无效: {}", id);
            return;
        }
        try {
            Long result = redisTemplate.opsForHash().delete(tableName, id);
            boolean success = result > 0;
            if (success) {
                log.debug("删除轮播数据成功，ID: {}", id);
            } else {
                log.warn("删除轮播数据失败，数据不存在，ID: {}", id);
            }
        } catch (Exception e) {
            log.error("删除轮播数据异常，ID: {}", id, e);
        }
    }

    /**
     * 清空所有轮播数据
     */
    public void clearAllCarousel() {
        try {
            redisTemplate.delete(tableName);
            log.debug("清空所有轮播数据成功");
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
     *
     * @param carouselList 新的轮播列表
     */
    public void sync(List<Carousel> carouselList) {
        if (carouselList == null || carouselList.isEmpty()) {
            return;
        }
        boolean tryLock = redisLock.tryLock(syncCompleteKey);
        if (tryLock) {
            try {
                List<Carousel> sourceList = getCarouselList();
                //新数据id集合
                Map<Long, Carousel> carouselMap = carouselList.stream()
                        .collect(Collectors.toMap(Carousel::getId, Function.identity(), (v1, v2) -> v2));
                log.info("开始同步轮播数据! nowSize={}, sourceSize={}", carouselList.size(), sourceList.size());
                List<CarouselUpdateInfo> syncList = new ArrayList<>();
                Carousel now;
                //清除旧数据
                clearAllCarousel();
                int delete = 0;
                int update = 0;
                for (Carousel source : sourceList) {
                    now = carouselMap.get(source.getId());
                    CarouselUpdateInfo carouselUpdateInfo = new CarouselUpdateInfo();
                    //检测数据变化
                    if (carouselMap.containsKey(source.getId())) {
                        //更新
                        if (!getMd5(now).equals(getMd5(source))) {
                            carouselUpdateInfo.setType(CarouselUpdateInfo.CarouselUpdateType.UPDATE);
                            carouselUpdateInfo.setCarousel(now);
                            syncList.add(carouselUpdateInfo);
                            //更新缓存数据
                            updateCarousel(now);
                            update++;
                        }
                    } else {
                        //已经删除的数据
                        carouselUpdateInfo.setType(CarouselUpdateInfo.CarouselUpdateType.DELETE);
                        carouselUpdateInfo.setCarousel(new Carousel(source.getId()));
                        syncList.add(carouselUpdateInfo);
                        //更新缓存数据
                        deleteCarouselById(source.getId());
                        delete++;
                    }
                }
                //检测是否有新增的数据
                Set<Long> sourceIdList = sourceList.stream()
                        .map(Carousel::getId)
                        .collect(Collectors.toSet());
                //新增的轮播数据
                List<CarouselUpdateInfo> addList = carouselList.stream()
                        .filter(p -> !sourceIdList.contains(p.getId()))
                        .peek(this::updateCarousel)
                        .map(carousel -> new CarouselUpdateInfo(CarouselUpdateInfo.CarouselUpdateType.UPDATE, carousel))
                        .toList();
                syncList.addAll(addList);
                //通知所有大厅节点更新数据
                notifyHallCarouselUpdate(syncList);
                log.info("轮播数据同步完毕!新增[{}]条，更新[{}]条，删除[{}]条!", addList.size(), update, delete);
            } catch (Exception e) {
                log.error("同步轮播数据错误!", e);
            } finally {
                redisLock.unlock(syncCompleteKey);
            }
        }
    }


}
