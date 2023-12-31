package site.btyhub.persistfirst;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.slf4j.event.Level;

/**
 * 类似本地消息表
 * @author: baotingyu
 * @date: 2023/12/4
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PersistFirst {


    String dataSourceBeanName() ;


}
