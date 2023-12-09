package site.btyhub.distributedlock;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import site.btyhub.util.AspectUtil;

/**
 * @author: baotingyu
 * @date: 2023/11/20
 **/
@Aspect
@Component
@Slf4j
@SuppressWarnings("all")
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class RedissonBasedDistributedLockAspect {
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    PropertyResolver propertyResolver;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

        if(!distributedLock.implementation().equals(this.getClass())){
            return joinPoint.proceed();
        }

        String springELName = distributedLock.name();
        // Spring EL
        String lockName = AspectUtil.resolveAopMethodSpringEL(joinPoint, springELName, String.class);

        Integer timeout = propertyResolver.getProperty(distributedLock.timeoutPropertyKey(), Integer.class, 3000);

        if (StringUtils.isEmpty(lockName) || Objects.isNull(timeout)) {
            throw new IllegalArgumentException("分布式锁参数不合法");
        }

        RLock lock = redissonClient.getLock(lockName);
        try {
            if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                log.info("获得分布式锁:{}", lockName);
                return joinPoint.proceed();
            }
            throw new RuntimeException("获取锁失败");
        } catch (Exception e) {
            log.error("{};入参:{};异常信息:{}", distributedLock.exceptionMsg(), Arrays.toString(joinPoint.getArgs()), e.getMessage(), e);
            throw e;
        } finally {
            if (Objects.nonNull(lock) && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
