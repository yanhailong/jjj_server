package com.jjg.game.core.entity;

import com.jjg.game.utils.LoggerUtils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GarbageCollectorInfo {

  private volatile long lastGcCount = 0;

  private volatile long lastGcTime = 0;

  private volatile long lastOldGcTime = 0;

  /* 在G1模式下, oldGcCount 即为Full gc的次数. CMS 模式下, 该值为cms + full gc 次数. */
  private volatile long lastOldGcCount = 0;

  private volatile long lastYoungGcTime = 0;

  private volatile long lastYoungGcCount = 0;

  public long getLastGcCount() {
    return this.lastGcCount;
  }

  public long getLastGcTime() {
    return this.lastGcTime;
  }

  public long getLastOldGcTime() {
    return this.lastOldGcTime;
  }

  public long getLastOldGcCount() {
    return this.lastOldGcCount;
  }

  public long getLastYounggcTime() {
    return this.lastYoungGcTime;
  }

  public long getLastYoungGcCount() {
    return this.lastYoungGcCount;
  }

  private Set<String> younggcAlgorithm =
      new LinkedHashSet<String>() {
        private static final long serialVersionUID = -4970284659158304450L;

        {
          // CMS->YOUNG
          add("ParNew");
          add("G1 Young Generation");
        }
      };

  private Set<String> oldgcAlgorithm =
      new LinkedHashSet<String>() {
        private static final long serialVersionUID = -5928912186336375797L;

        {
          add("ConcurrentMarkSweep");
          add("G1 Old Generation");
          add("G1 Concurrent GC");
        }
      };

  public synchronized Map<String, Number> collectGc() {
    //    long timebegin = System.currentTimeMillis();

    long gcCount = 0;
    long gcTime = 0;
    long oldGcCount = 0;
    long oldGcTime = 0;
    long youngGcCount = 0;
    long youngGcTime = 0;
    Map<String, Number> map = new LinkedHashMap<>();

    for (final GarbageCollectorMXBean garbageCollector :
        ManagementFactory.getGarbageCollectorMXBeans()) {

      gcTime += garbageCollector.getCollectionTime();
      gcCount += garbageCollector.getCollectionCount();
      String gcAlgorithm = garbageCollector.getName();

      if (younggcAlgorithm.contains(gcAlgorithm)) {
        youngGcTime += garbageCollector.getCollectionTime();
        youngGcCount += garbageCollector.getCollectionCount();
      } else if (oldgcAlgorithm.contains(gcAlgorithm)) {
        oldGcTime += garbageCollector.getCollectionTime();
        oldGcCount += garbageCollector.getCollectionCount();
      } else {
        LoggerUtils.LOGGER.error("UNKNOWN GarbageCollectorMXBean NAME:" + gcAlgorithm);
      }
    }

    //
    // GC实时统计信息
    //
    map.put("jvm.gc.count", gcCount - lastGcCount);
    map.put("jvm.gc.time", gcTime - lastGcTime);
    final long lastOldGcCountTemp = oldGcCount - lastOldGcCount;
    map.put("jvm.oldgc.count", lastOldGcCountTemp);
    map.put("jvm.oldgc.time", oldGcTime - lastOldGcTime);
    map.put("jvm.younggc.count", youngGcCount - lastYoungGcCount);
    map.put("jvm.younggc.time", youngGcTime - lastYoungGcTime);

    if (youngGcCount > lastYoungGcCount) {
      map.put(
          "jvm.younggc.meantime",
          (youngGcTime - lastYoungGcTime) / (youngGcCount - lastYoungGcCount));
    } else {
      map.put("jvm.younggc.meantime", 0);
    }

    lastGcCount = gcCount;
    lastGcTime = gcTime;
    lastYoungGcCount = youngGcCount;
    lastYoungGcTime = youngGcTime;
    lastOldGcCount = oldGcCount;
    lastOldGcTime = oldGcTime;

    return map;
  }
}
