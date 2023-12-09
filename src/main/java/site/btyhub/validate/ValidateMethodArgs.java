package site.btyhub.validate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link MethodParamBasedValidateAspect}
 * @author: baotingyu
 * @date: 2023/11/21
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ValidateMethodArgs {

    // 分组校验
    Class[] groups() default {};
}
