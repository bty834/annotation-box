package site.btyhub.batchrun;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO 分批执行工具
 * @author: baotingyu
 * @date: 2023/12/9
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface BatchRun {

    // Spring EL
    String batchArg();
    // Spring EL
    String batchSize();

    boolean continueOnException() default true;
}
