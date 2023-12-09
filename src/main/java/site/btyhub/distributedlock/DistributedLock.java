package site.btyhub.distributedlock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface DistributedLock {

    // Spring EL 锁名称
    String name();

    // 获取锁超时时间(单位ms)的 property key
    String timeoutPropertyKey() default "distributed.lock.timeout";

    // log.error
    String exceptionMsg() default "执行异常";

    // 默认redisson实现
    Class<?> implementation() default RedissonBasedDistributedLockAspect.class;
}
