package com.jjg.game.sampledata;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

// =================== 模板开始 ===================
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sampledata.container.*;
// =================== 模板结束 ===================
import com.jjg.game.sampledata.container.BaseCfgContainer.ContainerExceptionBlocker;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.processing.Generated;

/**
 * 游戏数据管理器
 *
 * @author auto_gen
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameDataManager {

  /** logger */
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /** cfgBean和container之间的绑定关系 */
  private Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> cfgBeanContainerRelator = Collections.emptyMap();

  /** 默认的监听器 */
  private static final IContainerEachLoadListener DEFAULT_LISTENER;

  /** 管理器是否加载所有配置成功 */
  private boolean loadAllFinished = false;

  static {
    DEFAULT_LISTENER =
        new IContainerEachLoadListener() {
          @Override
          public <T extends BaseCfgBean> void loadBefore(BaseCfgContainer<T> baseCfgContainer) {}

          @Override
          public <T extends BaseCfgBean> void loadAfter(BaseCfgContainer<T> baseCfgContainer) {}
        };
  }

  public GameDataManager() {}

  /**
   * 加载数据
   *
   * @param excelResourcePath excel资源路径
   * @throws Exception e
   */
  public static void loadAllData(String excelResourcePath) throws Exception {
    getInstance().loadAllDataByExcelPath(excelResourcePath, DEFAULT_LISTENER);
  }

  /**
   * 加载数据
   *
   * @param excelResourcePath excel资源路径
   * @param useMultiThreadAcc 是否使用多线程加速加载
   * @throws Exception e
   */
  public static void loadAllData(String excelResourcePath, boolean useMultiThreadAcc) throws Exception {
    getInstance().loadAllDataByExcelPath(excelResourcePath, DEFAULT_LISTENER, useMultiThreadAcc);
  }

  /**
   * 加载数据
   *
   * @param resourcePath excel资源路径
   * @param containerLoadListener 每个容器加载时调用
   * @throws Exception e
   */
  public static void loadAllData(
      String resourcePath, IContainerEachLoadListener containerLoadListener) throws Exception {
    getInstance().loadAllDataByExcelPath(resourcePath, containerLoadListener);
  }

  /**
   * 加载数据
   *
   * @param resourcePath excel资源路径
   * @param changedFileList 每个容器加载时调用
   * @throws Exception e
   */
  public static Set<Class<? extends BaseCfgBean>> loadDataByFileList(
      String resourcePath, List<File> changedFileList) throws Exception {
    return getInstance().loadDataByChangeFileList(resourcePath, changedFileList);
  }

  /**
   * 初始化所有容器, 如果配置表过多时可考虑使用反射实现
   *
   * @return 容器map
   */
  public Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> initAllContainer() {
    Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> containerMap = new ConcurrentHashMap<>(8);
    // region===============cfg加载模板开始===================
    containerMap.put(ActivityConfigCfg.class, new ActivityConfigCfgContainer());
    containerMap.put(AlbumCfg.class, new AlbumCfgContainer());
    containerMap.put(AuxiliaryAwardCfg.class, new AuxiliaryAwardCfgContainer());
    containerMap.put(AvatarCfg.class, new AvatarCfgContainer());
    containerMap.put(BaseElementCfg.class, new BaseElementCfgContainer());
    containerMap.put(BaseElementRewardCfg.class, new BaseElementRewardCfgContainer());
    containerMap.put(BaseInitCfg.class, new BaseInitCfgContainer());
    containerMap.put(BaseLineCfg.class, new BaseLineCfgContainer());
    containerMap.put(BaseRollerCfg.class, new BaseRollerCfgContainer());
    containerMap.put(BaseRoomCfg.class, new BaseRoomCfgContainer());
    containerMap.put(BetAreaCfg.class, new BetAreaCfgContainer());
    containerMap.put(BetRobotCfg.class, new BetRobotCfgContainer());
    containerMap.put(BlackjackCfg.class, new BlackjackCfgContainer());
    containerMap.put(BuildingFloorCfg.class, new BuildingFloorCfgContainer());
    containerMap.put(BuildingFunctionCfg.class, new BuildingFunctionCfgContainer());
    containerMap.put(BuildingGainCfg.class, new BuildingGainCfgContainer());
    containerMap.put(CashcowCfg.class, new CashcowCfgContainer());
    containerMap.put(ChessJackStrategyCfg.class, new ChessJackStrategyCfgContainer());
    containerMap.put(ChessRobotCfg.class, new ChessRobotCfgContainer());
    containerMap.put(ChessTexasStrategyCfg.class, new ChessTexasStrategyCfgContainer());
    containerMap.put(ConditionCfg.class, new ConditionCfgContainer());
    containerMap.put(ContinuouschargingCfg.class, new ContinuouschargingCfgContainer());
    containerMap.put(CumulativebenefitsCfg.class, new CumulativebenefitsCfgContainer());
    containerMap.put(DailyRechargeCfg.class, new DailyRechargeCfgContainer());
    containerMap.put(DailyRewardsCfg.class, new DailyRewardsCfgContainer());
    containerMap.put(DealerFunctionCfg.class, new DealerFunctionCfgContainer());
    containerMap.put(DropConfigCfg.class, new DropConfigCfgContainer());
    containerMap.put(DropDetailedCfg.class, new DropDetailedCfgContainer());
    containerMap.put(DropGroupCfg.class, new DropGroupCfgContainer());
    containerMap.put(FirstpaymentCfg.class, new FirstpaymentCfgContainer());
    containerMap.put(GameFunctionCfg.class, new GameFunctionCfgContainer());
    containerMap.put(GameListCfg.class, new GameListCfgContainer());
    containerMap.put(GiftPackCfg.class, new GiftPackCfgContainer());
    containerMap.put(GlobalConfigCfg.class, new GlobalConfigCfgContainer());
    containerMap.put(GrowthFundCfg.class, new GrowthFundCfgContainer());
    containerMap.put(IponeAreacodeConfigCfg.class, new IponeAreacodeConfigCfgContainer());
    containerMap.put(ItemCfg.class, new ItemCfgContainer());
    containerMap.put(LoginConfigCfg.class, new LoginConfigCfgContainer());
    containerMap.put(MGLuckyTreasureCfg.class, new MGLuckyTreasureCfgContainer());
    containerMap.put(MailCfg.class, new MailCfgContainer());
    containerMap.put(MiniGameCfg.class, new MiniGameCfgContainer());
    containerMap.put(MiniGameListCfg.class, new MiniGameListCfgContainer());
    containerMap.put(OfficialAwardsCfg.class, new OfficialAwardsCfgContainer());
    containerMap.put(PiggyBankCfg.class, new PiggyBankCfgContainer());
    containerMap.put(PlayerLevelConfigCfg.class, new PlayerLevelConfigCfgContainer());
    containerMap.put(PlayerLevelPackCfg.class, new PlayerLevelPackCfgContainer());
    containerMap.put(PointsAwardRankingCfg.class, new PointsAwardRankingCfgContainer());
    containerMap.put(PointsAwardRobotCfg.class, new PointsAwardRobotCfgContainer());
    containerMap.put(PointsAwardSigninCfg.class, new PointsAwardSigninCfgContainer());
    containerMap.put(PointsAwardTurntableCfg.class, new PointsAwardTurntableCfgContainer());
    containerMap.put(PokerPoolCfg.class, new PokerPoolCfgContainer());
    containerMap.put(PoolCfg.class, new PoolCfgContainer());
    containerMap.put(PopUpConfigCfg.class, new PopUpConfigCfgContainer());
    containerMap.put(PopUpGetWayCfg.class, new PopUpGetWayCfgContainer());
    containerMap.put(PrivilegeCardCfg.class, new PrivilegeCardCfgContainer());
    containerMap.put(RobotActionCfg.class, new RobotActionCfgContainer());
    containerMap.put(RobotCfg.class, new RobotCfgContainer());
    containerMap.put(RoomCfg.class, new RoomCfgContainer());
    containerMap.put(RoomExpendCfg.class, new RoomExpendCfgContainer());
    containerMap.put(Room_BetCfg.class, new Room_BetCfgContainer());
    containerMap.put(Room_ChessCfg.class, new Room_ChessCfgContainer());
    containerMap.put(RouletteShopCfg.class, new RouletteShopCfgContainer());
    containerMap.put(ScratchCardsCfg.class, new ScratchCardsCfgContainer());
    containerMap.put(SharePromoteCfg.class, new SharePromoteCfgContainer());
    containerMap.put(SouthernMoneyCfg.class, new SouthernMoneyCfgContainer());
    containerMap.put(SpecialAuxiliaryCfg.class, new SpecialAuxiliaryCfgContainer());
    containerMap.put(SpecialGirdCfg.class, new SpecialGirdCfgContainer());
    containerMap.put(SpecialModeCfg.class, new SpecialModeCfgContainer());
    containerMap.put(SpecialPlayCfg.class, new SpecialPlayCfgContainer());
    containerMap.put(SpecialResultLibCfg.class, new SpecialResultLibCfgContainer());
    containerMap.put(StatusCfg.class, new StatusCfgContainer());
    containerMap.put(TaskCfg.class, new TaskCfgContainer());
    containerMap.put(TexasCfg.class, new TexasCfgContainer());
    containerMap.put(UndergarmentCfg.class, new UndergarmentCfgContainer());
    containerMap.put(UpcomingMobileGameCfg.class, new UpcomingMobileGameCfgContainer());
    containerMap.put(ViplevelCfg.class, new ViplevelCfgContainer());
    containerMap.put(WarehouseCfg.class, new WarehouseCfgContainer());
    containerMap.put(WealthRouletteRewardCfg.class, new WealthRouletteRewardCfgContainer());
    containerMap.put(WinPosWeightCfg.class, new WinPosWeightCfgContainer());
    // endregion===============cfg加载模板结束===================
    return containerMap;
  }

  /** 加载每个数据时的数据监听器 */
  public interface IContainerEachLoadListener {

    /**
     * 加载之前
     *
     * @param baseCfgContainer 基础容器
     */
    <T extends BaseCfgBean> void loadBefore(BaseCfgContainer<T> baseCfgContainer);

    /**
     * 加载之后
     *
     * @param baseCfgContainer 容器
     * @throws Exception e
     */
    <T extends BaseCfgBean> void loadAfter(BaseCfgContainer<T> baseCfgContainer) throws Exception;
  }

  /** 最大核心线程数 需要预留一部分核心数给其他逻辑,但最低需要4个线程 */
  private static final int MAX_CORE_THREAD_NUM =
      Math.max(Runtime.getRuntime().availableProcessors() / 2, 4);

  /**
   * 获取线程池
   *
   * @param taskSize 需要加载的任务数量
   * @return 线程池
   */
  protected ThreadPoolExecutor getExcelLoadPoolExecutor(int taskSize){
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    // 最大容量
    int maxCapacity = taskSize - MAX_CORE_THREAD_NUM;
    AtomicInteger theadNumber = new AtomicInteger(0);
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    ThreadFactory threadFactory =
        r -> new Thread(threadGroup, r, "excel-loader-" + theadNumber.incrementAndGet());
    return new ThreadPoolExecutor(
        MAX_CORE_THREAD_NUM,
        availableProcessors,
        10,
        TimeUnit.SECONDS,
        new LinkedBlockingDeque<>(maxCapacity),
        threadFactory);
  }

  /**
   * 通过excel路径加速
   *
   * @param resourcePath excel资源路径
   * @param containerLoadListener 每个容器加载时调用
   * @throws Exception
   */
  public synchronized void loadAllDataByExcelPath(
      String resourcePath, IContainerEachLoadListener containerLoadListener) throws Exception {
    loadAllDataByExcelPath(resourcePath, containerLoadListener, false);
  }

  /**
   * 加载所有数据 同时只能允许一个线程进行加载
   *
   * @param resourcePath excel资源路径
   * @param containerLoadListener 每个容器加载时调用
   * @param useMultiThreadAcc 是否使用多线程加速加载excel数据
   */
  public synchronized void loadAllDataByExcelPath(
      String resourcePath,
      IContainerEachLoadListener containerLoadListener,
      boolean useMultiThreadAcc)
      throws Exception {
    final String checkedPath = checkAndGetAbsFilePath(resourcePath);
    Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> containerMap = initAllContainer();
    long loadStartTime = System.currentTimeMillis();
    // 异常列表
    List<Throwable> exceptions =
        useMultiThreadAcc ? new CopyOnWriteArrayList<>() : new ArrayList<>();
    if (useMultiThreadAcc) {
      ExecutorService executorService = getExcelLoadPoolExecutor(containerMap.size());
      CountDownLatch countDownLatch = new CountDownLatch(containerMap.size());
      for (Entry<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> containerEntry :
          containerMap.entrySet()) {
        BaseCfgContainer<?> cfgContainer = containerEntry.getValue();
        executorService.submit(
            () -> {
              loadSingleContainer(containerLoadListener, checkedPath, cfgContainer, exceptions);
              countDownLatch.countDown();
            });
      }
      // 每个配置表平均等待一秒钟
      if (!countDownLatch.await(containerMap.size(), TimeUnit.SECONDS)) {
        logger.error("配置表加载超时,数量: {}, 等待时间: {}s", containerMap.size(), containerMap.size());
        if (!executorService.isShutdown() && !executorService.isTerminated()) {
          executorService.shutdown();
        }
        throw new RuntimeException("配置表加载超时， 等待时间: " + containerMap.size() + "s, 当前预设配置表平均耗时1s");
      }
      if (!executorService.isShutdown() && !executorService.isTerminated()) {
        executorService.shutdown();
      }
    } else {
      for (Entry<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> containerEntry :
          containerMap.entrySet()) {
        BaseCfgContainer<?> cfgContainer = containerEntry.getValue();
        loadSingleContainer(containerLoadListener, checkedPath, cfgContainer, exceptions);
      }
    }
    if (!exceptions.isEmpty()) {
      // 整理和抛出异常
      throw new Exception(formatExceptionMsg(exceptions));
    } else {
      // 加载时内部没有异常时才算成功
      logger.info(
          "加载配置表成功,加载: {} 个文件共耗时: {}s",
          containerMap.size(),
          (System.currentTimeMillis() - loadStartTime) / 1000f);
      cfgBeanContainerRelator = Collections.unmodifiableMap(containerMap);
      // 标记加载完成
      loadAllFinished = true;
    }
  }

  /**
   * 检查文资源文件的合法性
   *
   * @param resourcePath 资源路径
   * @throws IOException IO异常
   */
  private String checkAndGetAbsFilePath(String resourcePath) throws IOException {
    String fullPath;
    // 如果是绝对路径
    if (resourcePath.startsWith("/") || resourcePath.indexOf(":") > 0) {
      fullPath = resourcePath;
    } else {
      fullPath = System.getProperty("user.dir") + "/" + resourcePath;
    }
    File file = new File(fullPath);
    if (!file.exists()) {
      throw new IOException("file: " + resourcePath + " not exists!");
    }
    if (!file.isDirectory()) {
      throw new IllegalArgumentException("resource path must be a directory!");
    }
    return file.getCanonicalPath();
  }

  /**
   * 通过路径加载单个容器
   * @param containerLoadListener 容器加载时的监听器
   * @param resourcePath excel路径
   * @param cfgContainer 配置表容器
   * @param exceptionCollector 异常收集器
   */
  private void loadSingleContainer(
      IContainerEachLoadListener containerLoadListener,
      String resourcePath,
      BaseCfgContainer<?> cfgContainer,
      List<Throwable> exceptionCollector) {
    try {
      logger.info("开始加载配置表: " + cfgContainer.getClass().getSimpleName());
      // 加载前调用
      containerLoadListener.loadBefore(cfgContainer);
      // 加载数据
      cfgContainer.loadData(resourcePath);
      // 加载后调用
      containerLoadListener.loadAfter(cfgContainer);
      logger.info(
          "配置表容器: "
              + cfgContainer.getClass().getSimpleName()
              + " 加载配置表: "
              + String.join(" ", cfgContainer.getExcelNameList())
              + "成功 加载文档数量: "
              + cfgContainer.getCfgBeanMap().size());
    } catch (Exception e) {
      if (e instanceof BaseCfgContainer.ContainerExceptionBlocker) {
        exceptionCollector.addAll(((ContainerExceptionBlocker) e).getExceptions());
      } else {
        exceptionCollector.add(e);
      }
      logger.error(
          "配置表容器: "
              + cfgContainer.getClass().getSimpleName()
              + " 加载配置表: "
              + String.join(" ", cfgContainer.getExcelNameList())
              + "失败");
    }
  }

  /**
   * 通过变化的excel加载数据
   *
   * @param resourcePath 资源路径
   * @param changeFileList 变化文件
   * @return 加载成功的配置列表
   */
  public synchronized Set<Class<? extends BaseCfgBean>> loadDataByChangeFileList(
      String resourcePath, List<File> changeFileList) throws Exception {
    Function<File, String> valueMapper =
        t -> {
          try {
            return DigestUtils.md5Hex(Files.newInputStream(t.toPath()));
          } catch (IOException e) {
            return "";
          }
        };
    // 变化的文件名和文件的MD5
    Map<String, String> fileNameAndMd5Map =
        changeFileList.stream().collect(Collectors.toMap(File::getName, valueMapper));
    List<String> changedFileNameList = new ArrayList<>(fileNameAndMd5Map.keySet());
    // 加载成功的列表
    Set<Class<? extends BaseCfgBean>> loadSuccessBeanClasses = new HashSet<>();
    // 获取需要重新加载的容器列表
    Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> needReloadContainer =
        getNeedReloadContainer(changedFileNameList);
    // 异常列表
    List<Throwable> exceptions = new ArrayList<>();
    // 复制一份新的
    Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> cfgBeanContainerRelatorTemp =
        new ConcurrentHashMap<>(cfgBeanContainerRelator);
    // 重载需要更新的配置数据
    for (Map.Entry<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> baseCfgContainer :
        needReloadContainer.entrySet()) {
      BaseCfgContainer<? extends BaseCfgBean> baseCfgContainerNew =
          baseCfgContainer.getValue().getNewContainer();
      boolean loadSuccess = false;
      try {
        // 通过容器加载excel文件
        loadSuccess =
            loadSingleChangeFile(
                resourcePath,
                baseCfgContainerNew,
                baseCfgContainer.getValue().getMd5CacheMap(),
                fileNameAndMd5Map);
      } catch (Exception e) {
        if (e instanceof BaseCfgContainer.ContainerExceptionBlocker) {
          exceptions.addAll(((ContainerExceptionBlocker) e).getExceptions());
        } else {
          exceptions.add(e);
        }
        logger.error(
            "配置表容器: "
                + baseCfgContainer.getValue().getClass().getSimpleName()
                + " 加载配置表: "
                + String.join(" ", baseCfgContainer.getValue().getExcelNameList())
                + "失败");
      }
      // 加载成功才写入
      if (loadSuccess) {
        cfgBeanContainerRelatorTemp.put(baseCfgContainer.getKey(), baseCfgContainerNew);
        loadSuccessBeanClasses.add(baseCfgContainer.getKey());
      }
    }
    // 为了不多次放表 如果有加载成功的还是将加载成功的表放入内存中
    if (!loadSuccessBeanClasses.isEmpty()) {
      cfgBeanContainerRelator = Collections.unmodifiableMap(cfgBeanContainerRelatorTemp);
      logger.info("配置表热更结束,替换配置表: " + String.join(",", changedFileNameList) + " 成功");
      return loadSuccessBeanClasses;
    }
    if(!exceptions.isEmpty()){
      throw new Exception(formatExceptionMsg(exceptions));
    }
    return loadSuccessBeanClasses;
  }

  /**
   * 获取需要重载的配置表容器
   *
   * @param changedFileNameList 文件改变列表
   * @return 需要重新加载的配置表容器
   */
  private Map<Class<? extends BaseCfgBean>, BaseCfgContainer<?>> getNeedReloadContainer(
      List<String> changedFileNameList) {
    return cfgBeanContainerRelator.entrySet().stream()
        .filter(
            baseCfgContainer ->
                changedFileNameList.stream()
                    .anyMatch(
                        (changedFileName) ->
                            baseCfgContainer.getValue().getExcelNameList().stream()
                                .anyMatch(
                                    (containerBindExcelName) ->
                                        containerBindExcelName.contains(changedFileName))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * 加载单个改变文件
   *
   * @param resourcePath 加载资源路径
   * @param baseCfgContainerNew 新容器
   * @param oldMd5CacheMap 旧容器的md5Map
   * @param newMd5CacheMap 新容器的md5Map
   * @return 是否加载成功
   * @throws Exception e
   */
  private boolean loadSingleChangeFile(
      String resourcePath,
      BaseCfgContainer<?> baseCfgContainerNew,
      Map<String, String> oldMd5CacheMap,
      Map<String, String> newMd5CacheMap)
      throws Exception {
    // 先判断是否有关联表,没有则直接更新
    if (!baseCfgContainerNew.hasRelatedTable()) {
      String excelName = baseCfgContainerNew.getExcelNameList().get(0);
      excelName =
          !excelName.contains("/") ? excelName : excelName.substring(excelName.indexOf("/") + 1);
      excelName =
          !excelName.contains("\\") ? excelName : excelName.substring(excelName.indexOf("\\") + 1);
      String newMd5Code = newMd5CacheMap.get(excelName);
      boolean isNewMd5CodeEmpty = newMd5Code == null || newMd5Code.isEmpty();
      // 包含则说明md5码没有变化不执行更新
      if (isNewMd5CodeEmpty || !oldMd5CacheMap.containsValue(newMd5Code)) {
        baseCfgContainerNew.loadData(resourcePath);
        logger.info(
            "配置表容器: {} 加载配置表: {} 成功",
            baseCfgContainerNew.getClass().getSimpleName(),
            String.join(" ", baseCfgContainerNew.getExcelNameList()));
        return true;
      } else {
        logger.warn(
            "配置表容器: "
                + baseCfgContainerNew.getClass().getSimpleName()
                + " 加载配置表: "
                + String.join(" ", baseCfgContainerNew.getExcelNameList())
                + " 跳过,新文件Md5码: {},老文件Md5码: {} 文件名: {}",
            newMd5Code,
            oldMd5CacheMap.get(excelName),
            excelName);
      }
    } else {
      // 如果是有关联型的配置则直接走逻辑更新
      baseCfgContainerNew.loadData(resourcePath);
      logger.info(
          "配置表容器: {} 加载配置表: {} 成功",
          baseCfgContainerNew.getClass().getSimpleName(),
          String.join(" ", baseCfgContainerNew.getExcelNameList()));
      return true;
    }
    return false;
  }

  /**
   * 异常整理并将异常抛出
   *
   * @param exceptions 异常列表
   */
  private String formatExceptionMsg(List<Throwable> exceptions) {
    // 整理配置表异常统一抛出异常
    StringBuilder exceptionBuilder = new StringBuilder();
    try {
      for (Throwable exception : exceptions) {
        if (exception instanceof BaseCfgContainer.ExcelDataParseException
            || exception instanceof BaseCfgContainer.ExcelFormatException) {
          // 内部异常优先打印
          exceptionBuilder = new StringBuilder(exception.getMessage() + "\n" + exceptionBuilder);
          continue;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        exception.printStackTrace(pw);
        exceptionBuilder.append(sw.getBuffer().toString()).append("\n");
        sw.close();
        pw.close();
      }
    } catch (IOException e) {
      exceptionBuilder.append(e);
    }
    return exceptionBuilder.toString();
  }

  /**
   * 通过配置表bean获取配置表容器
   *
   * @param baseCfgBeanClass 配置表beanClass
   * @return 配置表容器
   * @param <T> 容器类型
   * @param <Bfg> 配置表类型
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseCfgContainer<Bfg>, Bfg extends BaseCfgBean> T getCfgContainer(
      Class<Bfg> baseCfgBeanClass) {
    if (cfgBeanContainerRelator.containsKey(baseCfgBeanClass)) {
      return (T) cfgBeanContainerRelator.get(baseCfgBeanClass);
    } else {
      throw new RuntimeException(
          "can`t found cfg genpackage.bean class: " + baseCfgBeanClass.getSimpleName() + " genpackage.container");
    }
  }

  /**
   * 通过配置表bean判断是否有此容器
   *
   * @param baseCfgBeanClass 配置表beanClass
   * @param <T> 容器类型
   * @param <Bfg> 配置表类型
   * @return 配置表容器
   */
  @SuppressWarnings("unchecked")
  public <Bfg extends BaseCfgBean> boolean hasCfgContainer(Class<Bfg> baseCfgBeanClass) {
    return cfgBeanContainerRelator.containsKey(baseCfgBeanClass);
  }

  /**
   * 管理器是否加载完成
   *
   * @return
   */
  public boolean isLoadAllFinished() {
    return loadAllFinished;
  }

  /**
   * 获取所有缓存的 cfgBean class
   *
   * @return class列表
   */
  public Set<Class<? extends BaseCfgBean>> getAllCfgBeanClasses() {
    return Collections.unmodifiableSet(new HashSet<>(cfgBeanContainerRelator.keySet()));
  }

  // region===============cfg获取方法模板开始===================

  public static ActivityConfigCfg getActivityConfigCfg(int key) {
    return getInstance().getCfgContainer(ActivityConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ActivityConfigCfg> getActivityConfigCfgMap() {
    return getInstance().getCfgContainer(ActivityConfigCfg.class).getCfgBeanMap();
  }

  public static List<ActivityConfigCfg> getActivityConfigCfgList() {
    return getInstance().getCfgContainer(ActivityConfigCfg.class).getCfgBeanList();
  }

  public static AlbumCfg getAlbumCfg(int key) {
    return getInstance().getCfgContainer(AlbumCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, AlbumCfg> getAlbumCfgMap() {
    return getInstance().getCfgContainer(AlbumCfg.class).getCfgBeanMap();
  }

  public static List<AlbumCfg> getAlbumCfgList() {
    return getInstance().getCfgContainer(AlbumCfg.class).getCfgBeanList();
  }

  public static AuxiliaryAwardCfg getAuxiliaryAwardCfg(int key) {
    return getInstance().getCfgContainer(AuxiliaryAwardCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, AuxiliaryAwardCfg> getAuxiliaryAwardCfgMap() {
    return getInstance().getCfgContainer(AuxiliaryAwardCfg.class).getCfgBeanMap();
  }

  public static List<AuxiliaryAwardCfg> getAuxiliaryAwardCfgList() {
    return getInstance().getCfgContainer(AuxiliaryAwardCfg.class).getCfgBeanList();
  }

  public static AvatarCfg getAvatarCfg(int key) {
    return getInstance().getCfgContainer(AvatarCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, AvatarCfg> getAvatarCfgMap() {
    return getInstance().getCfgContainer(AvatarCfg.class).getCfgBeanMap();
  }

  public static List<AvatarCfg> getAvatarCfgList() {
    return getInstance().getCfgContainer(AvatarCfg.class).getCfgBeanList();
  }

  public static BaseElementCfg getBaseElementCfg(int key) {
    return getInstance().getCfgContainer(BaseElementCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseElementCfg> getBaseElementCfgMap() {
    return getInstance().getCfgContainer(BaseElementCfg.class).getCfgBeanMap();
  }

  public static List<BaseElementCfg> getBaseElementCfgList() {
    return getInstance().getCfgContainer(BaseElementCfg.class).getCfgBeanList();
  }

  public static BaseElementRewardCfg getBaseElementRewardCfg(int key) {
    return getInstance().getCfgContainer(BaseElementRewardCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseElementRewardCfg> getBaseElementRewardCfgMap() {
    return getInstance().getCfgContainer(BaseElementRewardCfg.class).getCfgBeanMap();
  }

  public static List<BaseElementRewardCfg> getBaseElementRewardCfgList() {
    return getInstance().getCfgContainer(BaseElementRewardCfg.class).getCfgBeanList();
  }

  public static BaseInitCfg getBaseInitCfg(int key) {
    return getInstance().getCfgContainer(BaseInitCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseInitCfg> getBaseInitCfgMap() {
    return getInstance().getCfgContainer(BaseInitCfg.class).getCfgBeanMap();
  }

  public static List<BaseInitCfg> getBaseInitCfgList() {
    return getInstance().getCfgContainer(BaseInitCfg.class).getCfgBeanList();
  }

  public static BaseLineCfg getBaseLineCfg(int key) {
    return getInstance().getCfgContainer(BaseLineCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseLineCfg> getBaseLineCfgMap() {
    return getInstance().getCfgContainer(BaseLineCfg.class).getCfgBeanMap();
  }

  public static List<BaseLineCfg> getBaseLineCfgList() {
    return getInstance().getCfgContainer(BaseLineCfg.class).getCfgBeanList();
  }

  public static BaseRollerCfg getBaseRollerCfg(int key) {
    return getInstance().getCfgContainer(BaseRollerCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseRollerCfg> getBaseRollerCfgMap() {
    return getInstance().getCfgContainer(BaseRollerCfg.class).getCfgBeanMap();
  }

  public static List<BaseRollerCfg> getBaseRollerCfgList() {
    return getInstance().getCfgContainer(BaseRollerCfg.class).getCfgBeanList();
  }

  public static BaseRoomCfg getBaseRoomCfg(int key) {
    return getInstance().getCfgContainer(BaseRoomCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BaseRoomCfg> getBaseRoomCfgMap() {
    return getInstance().getCfgContainer(BaseRoomCfg.class).getCfgBeanMap();
  }

  public static List<BaseRoomCfg> getBaseRoomCfgList() {
    return getInstance().getCfgContainer(BaseRoomCfg.class).getCfgBeanList();
  }

  public static BetAreaCfg getBetAreaCfg(int key) {
    return getInstance().getCfgContainer(BetAreaCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BetAreaCfg> getBetAreaCfgMap() {
    return getInstance().getCfgContainer(BetAreaCfg.class).getCfgBeanMap();
  }

  public static List<BetAreaCfg> getBetAreaCfgList() {
    return getInstance().getCfgContainer(BetAreaCfg.class).getCfgBeanList();
  }

  public static BetRobotCfg getBetRobotCfg(int key) {
    return getInstance().getCfgContainer(BetRobotCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BetRobotCfg> getBetRobotCfgMap() {
    return getInstance().getCfgContainer(BetRobotCfg.class).getCfgBeanMap();
  }

  public static List<BetRobotCfg> getBetRobotCfgList() {
    return getInstance().getCfgContainer(BetRobotCfg.class).getCfgBeanList();
  }

  public static BlackjackCfg getBlackjackCfg(int key) {
    return getInstance().getCfgContainer(BlackjackCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BlackjackCfg> getBlackjackCfgMap() {
    return getInstance().getCfgContainer(BlackjackCfg.class).getCfgBeanMap();
  }

  public static List<BlackjackCfg> getBlackjackCfgList() {
    return getInstance().getCfgContainer(BlackjackCfg.class).getCfgBeanList();
  }

  public static BuildingFloorCfg getBuildingFloorCfg(int key) {
    return getInstance().getCfgContainer(BuildingFloorCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BuildingFloorCfg> getBuildingFloorCfgMap() {
    return getInstance().getCfgContainer(BuildingFloorCfg.class).getCfgBeanMap();
  }

  public static List<BuildingFloorCfg> getBuildingFloorCfgList() {
    return getInstance().getCfgContainer(BuildingFloorCfg.class).getCfgBeanList();
  }

  public static BuildingFunctionCfg getBuildingFunctionCfg(int key) {
    return getInstance().getCfgContainer(BuildingFunctionCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BuildingFunctionCfg> getBuildingFunctionCfgMap() {
    return getInstance().getCfgContainer(BuildingFunctionCfg.class).getCfgBeanMap();
  }

  public static List<BuildingFunctionCfg> getBuildingFunctionCfgList() {
    return getInstance().getCfgContainer(BuildingFunctionCfg.class).getCfgBeanList();
  }

  public static BuildingGainCfg getBuildingGainCfg(int key) {
    return getInstance().getCfgContainer(BuildingGainCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, BuildingGainCfg> getBuildingGainCfgMap() {
    return getInstance().getCfgContainer(BuildingGainCfg.class).getCfgBeanMap();
  }

  public static List<BuildingGainCfg> getBuildingGainCfgList() {
    return getInstance().getCfgContainer(BuildingGainCfg.class).getCfgBeanList();
  }

  public static CashcowCfg getCashcowCfg(int key) {
    return getInstance().getCfgContainer(CashcowCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, CashcowCfg> getCashcowCfgMap() {
    return getInstance().getCfgContainer(CashcowCfg.class).getCfgBeanMap();
  }

  public static List<CashcowCfg> getCashcowCfgList() {
    return getInstance().getCfgContainer(CashcowCfg.class).getCfgBeanList();
  }

  public static ChessJackStrategyCfg getChessJackStrategyCfg(int key) {
    return getInstance().getCfgContainer(ChessJackStrategyCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ChessJackStrategyCfg> getChessJackStrategyCfgMap() {
    return getInstance().getCfgContainer(ChessJackStrategyCfg.class).getCfgBeanMap();
  }

  public static List<ChessJackStrategyCfg> getChessJackStrategyCfgList() {
    return getInstance().getCfgContainer(ChessJackStrategyCfg.class).getCfgBeanList();
  }

  public static ChessRobotCfg getChessRobotCfg(int key) {
    return getInstance().getCfgContainer(ChessRobotCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ChessRobotCfg> getChessRobotCfgMap() {
    return getInstance().getCfgContainer(ChessRobotCfg.class).getCfgBeanMap();
  }

  public static List<ChessRobotCfg> getChessRobotCfgList() {
    return getInstance().getCfgContainer(ChessRobotCfg.class).getCfgBeanList();
  }

  public static ChessTexasStrategyCfg getChessTexasStrategyCfg(int key) {
    return getInstance().getCfgContainer(ChessTexasStrategyCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ChessTexasStrategyCfg> getChessTexasStrategyCfgMap() {
    return getInstance().getCfgContainer(ChessTexasStrategyCfg.class).getCfgBeanMap();
  }

  public static List<ChessTexasStrategyCfg> getChessTexasStrategyCfgList() {
    return getInstance().getCfgContainer(ChessTexasStrategyCfg.class).getCfgBeanList();
  }

  public static ConditionCfg getConditionCfg(int key) {
    return getInstance().getCfgContainer(ConditionCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ConditionCfg> getConditionCfgMap() {
    return getInstance().getCfgContainer(ConditionCfg.class).getCfgBeanMap();
  }

  public static List<ConditionCfg> getConditionCfgList() {
    return getInstance().getCfgContainer(ConditionCfg.class).getCfgBeanList();
  }

  public static ContinuouschargingCfg getContinuouschargingCfg(int key) {
    return getInstance().getCfgContainer(ContinuouschargingCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ContinuouschargingCfg> getContinuouschargingCfgMap() {
    return getInstance().getCfgContainer(ContinuouschargingCfg.class).getCfgBeanMap();
  }

  public static List<ContinuouschargingCfg> getContinuouschargingCfgList() {
    return getInstance().getCfgContainer(ContinuouschargingCfg.class).getCfgBeanList();
  }

  public static CumulativebenefitsCfg getCumulativebenefitsCfg(int key) {
    return getInstance().getCfgContainer(CumulativebenefitsCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, CumulativebenefitsCfg> getCumulativebenefitsCfgMap() {
    return getInstance().getCfgContainer(CumulativebenefitsCfg.class).getCfgBeanMap();
  }

  public static List<CumulativebenefitsCfg> getCumulativebenefitsCfgList() {
    return getInstance().getCfgContainer(CumulativebenefitsCfg.class).getCfgBeanList();
  }

  public static DailyRechargeCfg getDailyRechargeCfg(int key) {
    return getInstance().getCfgContainer(DailyRechargeCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DailyRechargeCfg> getDailyRechargeCfgMap() {
    return getInstance().getCfgContainer(DailyRechargeCfg.class).getCfgBeanMap();
  }

  public static List<DailyRechargeCfg> getDailyRechargeCfgList() {
    return getInstance().getCfgContainer(DailyRechargeCfg.class).getCfgBeanList();
  }

  public static DailyRewardsCfg getDailyRewardsCfg(int key) {
    return getInstance().getCfgContainer(DailyRewardsCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DailyRewardsCfg> getDailyRewardsCfgMap() {
    return getInstance().getCfgContainer(DailyRewardsCfg.class).getCfgBeanMap();
  }

  public static List<DailyRewardsCfg> getDailyRewardsCfgList() {
    return getInstance().getCfgContainer(DailyRewardsCfg.class).getCfgBeanList();
  }

  public static DealerFunctionCfg getDealerFunctionCfg(int key) {
    return getInstance().getCfgContainer(DealerFunctionCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DealerFunctionCfg> getDealerFunctionCfgMap() {
    return getInstance().getCfgContainer(DealerFunctionCfg.class).getCfgBeanMap();
  }

  public static List<DealerFunctionCfg> getDealerFunctionCfgList() {
    return getInstance().getCfgContainer(DealerFunctionCfg.class).getCfgBeanList();
  }

  public static DropConfigCfg getDropConfigCfg(int key) {
    return getInstance().getCfgContainer(DropConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DropConfigCfg> getDropConfigCfgMap() {
    return getInstance().getCfgContainer(DropConfigCfg.class).getCfgBeanMap();
  }

  public static List<DropConfigCfg> getDropConfigCfgList() {
    return getInstance().getCfgContainer(DropConfigCfg.class).getCfgBeanList();
  }

  public static DropDetailedCfg getDropDetailedCfg(int key) {
    return getInstance().getCfgContainer(DropDetailedCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DropDetailedCfg> getDropDetailedCfgMap() {
    return getInstance().getCfgContainer(DropDetailedCfg.class).getCfgBeanMap();
  }

  public static List<DropDetailedCfg> getDropDetailedCfgList() {
    return getInstance().getCfgContainer(DropDetailedCfg.class).getCfgBeanList();
  }

  public static DropGroupCfg getDropGroupCfg(int key) {
    return getInstance().getCfgContainer(DropGroupCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, DropGroupCfg> getDropGroupCfgMap() {
    return getInstance().getCfgContainer(DropGroupCfg.class).getCfgBeanMap();
  }

  public static List<DropGroupCfg> getDropGroupCfgList() {
    return getInstance().getCfgContainer(DropGroupCfg.class).getCfgBeanList();
  }

  public static FirstpaymentCfg getFirstpaymentCfg(int key) {
    return getInstance().getCfgContainer(FirstpaymentCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, FirstpaymentCfg> getFirstpaymentCfgMap() {
    return getInstance().getCfgContainer(FirstpaymentCfg.class).getCfgBeanMap();
  }

  public static List<FirstpaymentCfg> getFirstpaymentCfgList() {
    return getInstance().getCfgContainer(FirstpaymentCfg.class).getCfgBeanList();
  }

  public static GameFunctionCfg getGameFunctionCfg(int key) {
    return getInstance().getCfgContainer(GameFunctionCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, GameFunctionCfg> getGameFunctionCfgMap() {
    return getInstance().getCfgContainer(GameFunctionCfg.class).getCfgBeanMap();
  }

  public static List<GameFunctionCfg> getGameFunctionCfgList() {
    return getInstance().getCfgContainer(GameFunctionCfg.class).getCfgBeanList();
  }

  public static GameListCfg getGameListCfg(int key) {
    return getInstance().getCfgContainer(GameListCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, GameListCfg> getGameListCfgMap() {
    return getInstance().getCfgContainer(GameListCfg.class).getCfgBeanMap();
  }

  public static List<GameListCfg> getGameListCfgList() {
    return getInstance().getCfgContainer(GameListCfg.class).getCfgBeanList();
  }

  public static GiftPackCfg getGiftPackCfg(int key) {
    return getInstance().getCfgContainer(GiftPackCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, GiftPackCfg> getGiftPackCfgMap() {
    return getInstance().getCfgContainer(GiftPackCfg.class).getCfgBeanMap();
  }

  public static List<GiftPackCfg> getGiftPackCfgList() {
    return getInstance().getCfgContainer(GiftPackCfg.class).getCfgBeanList();
  }

  public static GlobalConfigCfg getGlobalConfigCfg(int key) {
    return getInstance().getCfgContainer(GlobalConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, GlobalConfigCfg> getGlobalConfigCfgMap() {
    return getInstance().getCfgContainer(GlobalConfigCfg.class).getCfgBeanMap();
  }

  public static List<GlobalConfigCfg> getGlobalConfigCfgList() {
    return getInstance().getCfgContainer(GlobalConfigCfg.class).getCfgBeanList();
  }

  public static GrowthFundCfg getGrowthFundCfg(int key) {
    return getInstance().getCfgContainer(GrowthFundCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, GrowthFundCfg> getGrowthFundCfgMap() {
    return getInstance().getCfgContainer(GrowthFundCfg.class).getCfgBeanMap();
  }

  public static List<GrowthFundCfg> getGrowthFundCfgList() {
    return getInstance().getCfgContainer(GrowthFundCfg.class).getCfgBeanList();
  }

  public static IponeAreacodeConfigCfg getIponeAreacodeConfigCfg(int key) {
    return getInstance().getCfgContainer(IponeAreacodeConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, IponeAreacodeConfigCfg> getIponeAreacodeConfigCfgMap() {
    return getInstance().getCfgContainer(IponeAreacodeConfigCfg.class).getCfgBeanMap();
  }

  public static List<IponeAreacodeConfigCfg> getIponeAreacodeConfigCfgList() {
    return getInstance().getCfgContainer(IponeAreacodeConfigCfg.class).getCfgBeanList();
  }

  public static ItemCfg getItemCfg(int key) {
    return getInstance().getCfgContainer(ItemCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ItemCfg> getItemCfgMap() {
    return getInstance().getCfgContainer(ItemCfg.class).getCfgBeanMap();
  }

  public static List<ItemCfg> getItemCfgList() {
    return getInstance().getCfgContainer(ItemCfg.class).getCfgBeanList();
  }

  public static LoginConfigCfg getLoginConfigCfg(int key) {
    return getInstance().getCfgContainer(LoginConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, LoginConfigCfg> getLoginConfigCfgMap() {
    return getInstance().getCfgContainer(LoginConfigCfg.class).getCfgBeanMap();
  }

  public static List<LoginConfigCfg> getLoginConfigCfgList() {
    return getInstance().getCfgContainer(LoginConfigCfg.class).getCfgBeanList();
  }

  public static MGLuckyTreasureCfg getMGLuckyTreasureCfg(int key) {
    return getInstance().getCfgContainer(MGLuckyTreasureCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, MGLuckyTreasureCfg> getMGLuckyTreasureCfgMap() {
    return getInstance().getCfgContainer(MGLuckyTreasureCfg.class).getCfgBeanMap();
  }

  public static List<MGLuckyTreasureCfg> getMGLuckyTreasureCfgList() {
    return getInstance().getCfgContainer(MGLuckyTreasureCfg.class).getCfgBeanList();
  }

  public static MailCfg getMailCfg(int key) {
    return getInstance().getCfgContainer(MailCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, MailCfg> getMailCfgMap() {
    return getInstance().getCfgContainer(MailCfg.class).getCfgBeanMap();
  }

  public static List<MailCfg> getMailCfgList() {
    return getInstance().getCfgContainer(MailCfg.class).getCfgBeanList();
  }

  public static MiniGameCfg getMiniGameCfg(int key) {
    return getInstance().getCfgContainer(MiniGameCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, MiniGameCfg> getMiniGameCfgMap() {
    return getInstance().getCfgContainer(MiniGameCfg.class).getCfgBeanMap();
  }

  public static List<MiniGameCfg> getMiniGameCfgList() {
    return getInstance().getCfgContainer(MiniGameCfg.class).getCfgBeanList();
  }

  public static MiniGameListCfg getMiniGameListCfg(int key) {
    return getInstance().getCfgContainer(MiniGameListCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, MiniGameListCfg> getMiniGameListCfgMap() {
    return getInstance().getCfgContainer(MiniGameListCfg.class).getCfgBeanMap();
  }

  public static List<MiniGameListCfg> getMiniGameListCfgList() {
    return getInstance().getCfgContainer(MiniGameListCfg.class).getCfgBeanList();
  }

  public static OfficialAwardsCfg getOfficialAwardsCfg(int key) {
    return getInstance().getCfgContainer(OfficialAwardsCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, OfficialAwardsCfg> getOfficialAwardsCfgMap() {
    return getInstance().getCfgContainer(OfficialAwardsCfg.class).getCfgBeanMap();
  }

  public static List<OfficialAwardsCfg> getOfficialAwardsCfgList() {
    return getInstance().getCfgContainer(OfficialAwardsCfg.class).getCfgBeanList();
  }

  public static PiggyBankCfg getPiggyBankCfg(int key) {
    return getInstance().getCfgContainer(PiggyBankCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PiggyBankCfg> getPiggyBankCfgMap() {
    return getInstance().getCfgContainer(PiggyBankCfg.class).getCfgBeanMap();
  }

  public static List<PiggyBankCfg> getPiggyBankCfgList() {
    return getInstance().getCfgContainer(PiggyBankCfg.class).getCfgBeanList();
  }

  public static PlayerLevelConfigCfg getPlayerLevelConfigCfg(int key) {
    return getInstance().getCfgContainer(PlayerLevelConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PlayerLevelConfigCfg> getPlayerLevelConfigCfgMap() {
    return getInstance().getCfgContainer(PlayerLevelConfigCfg.class).getCfgBeanMap();
  }

  public static List<PlayerLevelConfigCfg> getPlayerLevelConfigCfgList() {
    return getInstance().getCfgContainer(PlayerLevelConfigCfg.class).getCfgBeanList();
  }

  public static PlayerLevelPackCfg getPlayerLevelPackCfg(int key) {
    return getInstance().getCfgContainer(PlayerLevelPackCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PlayerLevelPackCfg> getPlayerLevelPackCfgMap() {
    return getInstance().getCfgContainer(PlayerLevelPackCfg.class).getCfgBeanMap();
  }

  public static List<PlayerLevelPackCfg> getPlayerLevelPackCfgList() {
    return getInstance().getCfgContainer(PlayerLevelPackCfg.class).getCfgBeanList();
  }

  public static PointsAwardRankingCfg getPointsAwardRankingCfg(int key) {
    return getInstance().getCfgContainer(PointsAwardRankingCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PointsAwardRankingCfg> getPointsAwardRankingCfgMap() {
    return getInstance().getCfgContainer(PointsAwardRankingCfg.class).getCfgBeanMap();
  }

  public static List<PointsAwardRankingCfg> getPointsAwardRankingCfgList() {
    return getInstance().getCfgContainer(PointsAwardRankingCfg.class).getCfgBeanList();
  }

  public static PointsAwardRobotCfg getPointsAwardRobotCfg(int key) {
    return getInstance().getCfgContainer(PointsAwardRobotCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PointsAwardRobotCfg> getPointsAwardRobotCfgMap() {
    return getInstance().getCfgContainer(PointsAwardRobotCfg.class).getCfgBeanMap();
  }

  public static List<PointsAwardRobotCfg> getPointsAwardRobotCfgList() {
    return getInstance().getCfgContainer(PointsAwardRobotCfg.class).getCfgBeanList();
  }

  public static PointsAwardSigninCfg getPointsAwardSigninCfg(int key) {
    return getInstance().getCfgContainer(PointsAwardSigninCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PointsAwardSigninCfg> getPointsAwardSigninCfgMap() {
    return getInstance().getCfgContainer(PointsAwardSigninCfg.class).getCfgBeanMap();
  }

  public static List<PointsAwardSigninCfg> getPointsAwardSigninCfgList() {
    return getInstance().getCfgContainer(PointsAwardSigninCfg.class).getCfgBeanList();
  }

  public static PointsAwardTurntableCfg getPointsAwardTurntableCfg(int key) {
    return getInstance().getCfgContainer(PointsAwardTurntableCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PointsAwardTurntableCfg> getPointsAwardTurntableCfgMap() {
    return getInstance().getCfgContainer(PointsAwardTurntableCfg.class).getCfgBeanMap();
  }

  public static List<PointsAwardTurntableCfg> getPointsAwardTurntableCfgList() {
    return getInstance().getCfgContainer(PointsAwardTurntableCfg.class).getCfgBeanList();
  }

  public static PokerPoolCfg getPokerPoolCfg(int key) {
    return getInstance().getCfgContainer(PokerPoolCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PokerPoolCfg> getPokerPoolCfgMap() {
    return getInstance().getCfgContainer(PokerPoolCfg.class).getCfgBeanMap();
  }

  public static List<PokerPoolCfg> getPokerPoolCfgList() {
    return getInstance().getCfgContainer(PokerPoolCfg.class).getCfgBeanList();
  }

  public static PoolCfg getPoolCfg(int key) {
    return getInstance().getCfgContainer(PoolCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PoolCfg> getPoolCfgMap() {
    return getInstance().getCfgContainer(PoolCfg.class).getCfgBeanMap();
  }

  public static List<PoolCfg> getPoolCfgList() {
    return getInstance().getCfgContainer(PoolCfg.class).getCfgBeanList();
  }

  public static PopUpConfigCfg getPopUpConfigCfg(int key) {
    return getInstance().getCfgContainer(PopUpConfigCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PopUpConfigCfg> getPopUpConfigCfgMap() {
    return getInstance().getCfgContainer(PopUpConfigCfg.class).getCfgBeanMap();
  }

  public static List<PopUpConfigCfg> getPopUpConfigCfgList() {
    return getInstance().getCfgContainer(PopUpConfigCfg.class).getCfgBeanList();
  }

  public static PopUpGetWayCfg getPopUpGetWayCfg(int key) {
    return getInstance().getCfgContainer(PopUpGetWayCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PopUpGetWayCfg> getPopUpGetWayCfgMap() {
    return getInstance().getCfgContainer(PopUpGetWayCfg.class).getCfgBeanMap();
  }

  public static List<PopUpGetWayCfg> getPopUpGetWayCfgList() {
    return getInstance().getCfgContainer(PopUpGetWayCfg.class).getCfgBeanList();
  }

  public static PrivilegeCardCfg getPrivilegeCardCfg(int key) {
    return getInstance().getCfgContainer(PrivilegeCardCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, PrivilegeCardCfg> getPrivilegeCardCfgMap() {
    return getInstance().getCfgContainer(PrivilegeCardCfg.class).getCfgBeanMap();
  }

  public static List<PrivilegeCardCfg> getPrivilegeCardCfgList() {
    return getInstance().getCfgContainer(PrivilegeCardCfg.class).getCfgBeanList();
  }

  public static RobotActionCfg getRobotActionCfg(int key) {
    return getInstance().getCfgContainer(RobotActionCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, RobotActionCfg> getRobotActionCfgMap() {
    return getInstance().getCfgContainer(RobotActionCfg.class).getCfgBeanMap();
  }

  public static List<RobotActionCfg> getRobotActionCfgList() {
    return getInstance().getCfgContainer(RobotActionCfg.class).getCfgBeanList();
  }

  public static RobotCfg getRobotCfg(int key) {
    return getInstance().getCfgContainer(RobotCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, RobotCfg> getRobotCfgMap() {
    return getInstance().getCfgContainer(RobotCfg.class).getCfgBeanMap();
  }

  public static List<RobotCfg> getRobotCfgList() {
    return getInstance().getCfgContainer(RobotCfg.class).getCfgBeanList();
  }

  public static RoomCfg getRoomCfg(int key) {
    return getInstance().getCfgContainer(RoomCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, RoomCfg> getRoomCfgMap() {
    return getInstance().getCfgContainer(RoomCfg.class).getCfgBeanMap();
  }

  public static List<RoomCfg> getRoomCfgList() {
    return getInstance().getCfgContainer(RoomCfg.class).getCfgBeanList();
  }

  public static RoomExpendCfg getRoomExpendCfg(int key) {
    return getInstance().getCfgContainer(RoomExpendCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, RoomExpendCfg> getRoomExpendCfgMap() {
    return getInstance().getCfgContainer(RoomExpendCfg.class).getCfgBeanMap();
  }

  public static List<RoomExpendCfg> getRoomExpendCfgList() {
    return getInstance().getCfgContainer(RoomExpendCfg.class).getCfgBeanList();
  }

  public static Room_BetCfg getRoom_BetCfg(int key) {
    return getInstance().getCfgContainer(Room_BetCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, Room_BetCfg> getRoom_BetCfgMap() {
    return getInstance().getCfgContainer(Room_BetCfg.class).getCfgBeanMap();
  }

  public static List<Room_BetCfg> getRoom_BetCfgList() {
    return getInstance().getCfgContainer(Room_BetCfg.class).getCfgBeanList();
  }

  public static Room_ChessCfg getRoom_ChessCfg(int key) {
    return getInstance().getCfgContainer(Room_ChessCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, Room_ChessCfg> getRoom_ChessCfgMap() {
    return getInstance().getCfgContainer(Room_ChessCfg.class).getCfgBeanMap();
  }

  public static List<Room_ChessCfg> getRoom_ChessCfgList() {
    return getInstance().getCfgContainer(Room_ChessCfg.class).getCfgBeanList();
  }

  public static RouletteShopCfg getRouletteShopCfg(int key) {
    return getInstance().getCfgContainer(RouletteShopCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, RouletteShopCfg> getRouletteShopCfgMap() {
    return getInstance().getCfgContainer(RouletteShopCfg.class).getCfgBeanMap();
  }

  public static List<RouletteShopCfg> getRouletteShopCfgList() {
    return getInstance().getCfgContainer(RouletteShopCfg.class).getCfgBeanList();
  }

  public static ScratchCardsCfg getScratchCardsCfg(int key) {
    return getInstance().getCfgContainer(ScratchCardsCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ScratchCardsCfg> getScratchCardsCfgMap() {
    return getInstance().getCfgContainer(ScratchCardsCfg.class).getCfgBeanMap();
  }

  public static List<ScratchCardsCfg> getScratchCardsCfgList() {
    return getInstance().getCfgContainer(ScratchCardsCfg.class).getCfgBeanList();
  }

  public static SharePromoteCfg getSharePromoteCfg(int key) {
    return getInstance().getCfgContainer(SharePromoteCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SharePromoteCfg> getSharePromoteCfgMap() {
    return getInstance().getCfgContainer(SharePromoteCfg.class).getCfgBeanMap();
  }

  public static List<SharePromoteCfg> getSharePromoteCfgList() {
    return getInstance().getCfgContainer(SharePromoteCfg.class).getCfgBeanList();
  }

  public static SouthernMoneyCfg getSouthernMoneyCfg(int key) {
    return getInstance().getCfgContainer(SouthernMoneyCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SouthernMoneyCfg> getSouthernMoneyCfgMap() {
    return getInstance().getCfgContainer(SouthernMoneyCfg.class).getCfgBeanMap();
  }

  public static List<SouthernMoneyCfg> getSouthernMoneyCfgList() {
    return getInstance().getCfgContainer(SouthernMoneyCfg.class).getCfgBeanList();
  }

  public static SpecialAuxiliaryCfg getSpecialAuxiliaryCfg(int key) {
    return getInstance().getCfgContainer(SpecialAuxiliaryCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SpecialAuxiliaryCfg> getSpecialAuxiliaryCfgMap() {
    return getInstance().getCfgContainer(SpecialAuxiliaryCfg.class).getCfgBeanMap();
  }

  public static List<SpecialAuxiliaryCfg> getSpecialAuxiliaryCfgList() {
    return getInstance().getCfgContainer(SpecialAuxiliaryCfg.class).getCfgBeanList();
  }

  public static SpecialGirdCfg getSpecialGirdCfg(int key) {
    return getInstance().getCfgContainer(SpecialGirdCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SpecialGirdCfg> getSpecialGirdCfgMap() {
    return getInstance().getCfgContainer(SpecialGirdCfg.class).getCfgBeanMap();
  }

  public static List<SpecialGirdCfg> getSpecialGirdCfgList() {
    return getInstance().getCfgContainer(SpecialGirdCfg.class).getCfgBeanList();
  }

  public static SpecialModeCfg getSpecialModeCfg(int key) {
    return getInstance().getCfgContainer(SpecialModeCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SpecialModeCfg> getSpecialModeCfgMap() {
    return getInstance().getCfgContainer(SpecialModeCfg.class).getCfgBeanMap();
  }

  public static List<SpecialModeCfg> getSpecialModeCfgList() {
    return getInstance().getCfgContainer(SpecialModeCfg.class).getCfgBeanList();
  }

  public static SpecialPlayCfg getSpecialPlayCfg(int key) {
    return getInstance().getCfgContainer(SpecialPlayCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SpecialPlayCfg> getSpecialPlayCfgMap() {
    return getInstance().getCfgContainer(SpecialPlayCfg.class).getCfgBeanMap();
  }

  public static List<SpecialPlayCfg> getSpecialPlayCfgList() {
    return getInstance().getCfgContainer(SpecialPlayCfg.class).getCfgBeanList();
  }

  public static SpecialResultLibCfg getSpecialResultLibCfg(int key) {
    return getInstance().getCfgContainer(SpecialResultLibCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, SpecialResultLibCfg> getSpecialResultLibCfgMap() {
    return getInstance().getCfgContainer(SpecialResultLibCfg.class).getCfgBeanMap();
  }

  public static List<SpecialResultLibCfg> getSpecialResultLibCfgList() {
    return getInstance().getCfgContainer(SpecialResultLibCfg.class).getCfgBeanList();
  }

  public static StatusCfg getStatusCfg(int key) {
    return getInstance().getCfgContainer(StatusCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, StatusCfg> getStatusCfgMap() {
    return getInstance().getCfgContainer(StatusCfg.class).getCfgBeanMap();
  }

  public static List<StatusCfg> getStatusCfgList() {
    return getInstance().getCfgContainer(StatusCfg.class).getCfgBeanList();
  }

  public static TaskCfg getTaskCfg(int key) {
    return getInstance().getCfgContainer(TaskCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, TaskCfg> getTaskCfgMap() {
    return getInstance().getCfgContainer(TaskCfg.class).getCfgBeanMap();
  }

  public static List<TaskCfg> getTaskCfgList() {
    return getInstance().getCfgContainer(TaskCfg.class).getCfgBeanList();
  }

  public static TexasCfg getTexasCfg(int key) {
    return getInstance().getCfgContainer(TexasCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, TexasCfg> getTexasCfgMap() {
    return getInstance().getCfgContainer(TexasCfg.class).getCfgBeanMap();
  }

  public static List<TexasCfg> getTexasCfgList() {
    return getInstance().getCfgContainer(TexasCfg.class).getCfgBeanList();
  }

  public static UndergarmentCfg getUndergarmentCfg(int key) {
    return getInstance().getCfgContainer(UndergarmentCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, UndergarmentCfg> getUndergarmentCfgMap() {
    return getInstance().getCfgContainer(UndergarmentCfg.class).getCfgBeanMap();
  }

  public static List<UndergarmentCfg> getUndergarmentCfgList() {
    return getInstance().getCfgContainer(UndergarmentCfg.class).getCfgBeanList();
  }

  public static UpcomingMobileGameCfg getUpcomingMobileGameCfg(int key) {
    return getInstance().getCfgContainer(UpcomingMobileGameCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, UpcomingMobileGameCfg> getUpcomingMobileGameCfgMap() {
    return getInstance().getCfgContainer(UpcomingMobileGameCfg.class).getCfgBeanMap();
  }

  public static List<UpcomingMobileGameCfg> getUpcomingMobileGameCfgList() {
    return getInstance().getCfgContainer(UpcomingMobileGameCfg.class).getCfgBeanList();
  }

  public static ViplevelCfg getViplevelCfg(int key) {
    return getInstance().getCfgContainer(ViplevelCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, ViplevelCfg> getViplevelCfgMap() {
    return getInstance().getCfgContainer(ViplevelCfg.class).getCfgBeanMap();
  }

  public static List<ViplevelCfg> getViplevelCfgList() {
    return getInstance().getCfgContainer(ViplevelCfg.class).getCfgBeanList();
  }

  public static WarehouseCfg getWarehouseCfg(int key) {
    return getInstance().getCfgContainer(WarehouseCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, WarehouseCfg> getWarehouseCfgMap() {
    return getInstance().getCfgContainer(WarehouseCfg.class).getCfgBeanMap();
  }

  public static List<WarehouseCfg> getWarehouseCfgList() {
    return getInstance().getCfgContainer(WarehouseCfg.class).getCfgBeanList();
  }

  public static WealthRouletteRewardCfg getWealthRouletteRewardCfg(int key) {
    return getInstance().getCfgContainer(WealthRouletteRewardCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, WealthRouletteRewardCfg> getWealthRouletteRewardCfgMap() {
    return getInstance().getCfgContainer(WealthRouletteRewardCfg.class).getCfgBeanMap();
  }

  public static List<WealthRouletteRewardCfg> getWealthRouletteRewardCfgList() {
    return getInstance().getCfgContainer(WealthRouletteRewardCfg.class).getCfgBeanList();
  }

  public static WinPosWeightCfg getWinPosWeightCfg(int key) {
    return getInstance().getCfgContainer(WinPosWeightCfg.class).getCfgBeanMap().get(key);
  }

  public static Map<Integer, WinPosWeightCfg> getWinPosWeightCfgMap() {
    return getInstance().getCfgContainer(WinPosWeightCfg.class).getCfgBeanMap();
  }

  public static List<WinPosWeightCfg> getWinPosWeightCfgList() {
    return getInstance().getCfgContainer(WinPosWeightCfg.class).getCfgBeanList();
  }


  // endregion===============cfg获取方法模板结束===================

  /**
   * 单例
   *
   * @return GameDataManager
   */
  public static GameDataManager getInstance() {
    return Singleton.INSTANCE.getInstance();
  }

  enum Singleton {
    // 单例
    INSTANCE;

    private final GameDataManager instance;

    Singleton() {
      this.instance = new GameDataManager();
    }

    public GameDataManager getInstance() {
      return instance;
    }
  }

  public static void main(String[] args) throws Exception {
    loadAllData("D:\\workspace\\number\\gamedoc-master\\gamedoc\\游戏配置表");
  }
}
