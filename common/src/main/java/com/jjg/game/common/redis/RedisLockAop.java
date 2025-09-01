package com.jjg.game.common.redis;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * redis锁切面，作用于需要对整个方法内的数据操作逻辑进行加锁的情形，支持重入
 *
 * @author 2CL
 */
@Aspect
@Component
public class RedisLockAop {

    private static final Logger log = LoggerFactory.getLogger(RedisLockAop.class);
    @Autowired
    private RedissonClient redisLock;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(redissonLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedissonLock redissonLock) throws Throwable {
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        // 解析key
        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
        Parameter[] parameters = method.getParameters();
        String[] paramsName = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(Param.class)) {
                Param param = parameter.getAnnotation(Param.class);
                paramsName[i] = param.value();
            } else {
                paramsName[i] = "arg" + i;
            }
            context.setVariable(paramsName[i], args[i]);
        }
        // 获取key名
        List<String> keys =
            Stream.of(redissonLock.keys())
                .map(k -> parser.parseExpression(k).getValue(context, String.class))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        keys.add(parser.parseExpression(redissonLock.key()).getValue(context, String.class));
        // 获取锁
        RLock lock;
        if (keys.size() == 1) {
            lock = this.redisLock.getLock(keys.getFirst());
        } else {
            // 如果是多个键，后续如果是多节点redis需要修改为红锁
            RLock[] locks = keys.stream().map(k -> this.redisLock.getLock(k)).toArray(RLock[]::new);
            lock = this.redisLock.getMultiLock(locks);
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(redissonLock.waitTime(), redissonLock.leaseTime(), redissonLock.timeUnit());
            if (locked) {
                // 调用逻辑
                return joinPoint.proceed();
            } else {
                // 处理获取锁失败的情况
                return handleFailed(redissonLock, joinPoint, method, startTime, args);
            }
        } catch (Exception exception) {
            log.error("尝试加锁: {} 失败：{}", String.join(",", paramsName), exception.getMessage(), exception);
            throw exception;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 加锁失败时，处理失败的情况
     *
     * @param redisLock 锁注解
     * @param joinPoint 切面
     * @param method    注解的方法
     * @param args      注解的方法参数
     * @return 回调执行结果
     * @throws Throwable e
     */
    private Object handleFailed(
        RedissonLock redisLock, ProceedingJoinPoint joinPoint, Method method, long startTime, Object[] args) throws Throwable {
        String keys = String.join(",", redisLock.keys()) + "," + redisLock.key();
        log.warn("获取锁：{} 失败，类：{} 方法：{} 等待时间：{}",
            keys,
            joinPoint.getTarget().getClass().getSimpleName(),
            method.getName(),
            (System.currentTimeMillis() - startTime));
        switch (redisLock.failedStrategy()) {
            case LockFailedStrategy.NONE:
                return null;
            case LockFailedStrategy.CALLBACK:
                String callback = redisLock.fallbackMethod();
                if (callback.isEmpty()) {
                    log.error("加锁获取锁失败时，置顶异常处理为回调，但回调函数为空");
                    throw new IllegalArgumentException("加锁获取锁失败时，置顶异常处理为回调，但回调函数为空");
                }
                Method fallbackMethod = joinPoint.getTarget().getClass()
                    .getMethod(callback, method.getParameterTypes());
                return fallbackMethod.invoke(joinPoint.getTarget(), args);
            case LockFailedStrategy.EXCEPTION:
                log.warn("获取锁：{} 超时", keys);
                throw new IllegalStateException("获取锁：" + keys + "超时");
            default:
                log.error("错误的加锁失败策略");
                throw new IllegalArgumentException("错误的加锁失败策略" + redisLock.failedStrategy());
        }
    }

    /**
     * 获取注解对应的方法
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        // 处理接口代理的情况
        try {
            return joinPoint.getTarget()
                .getClass()
                .getMethod(signatureMethod.getName(), signatureMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return signatureMethod;
        }
    }
}
