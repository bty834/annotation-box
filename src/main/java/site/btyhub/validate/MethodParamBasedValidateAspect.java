package site.btyhub.validate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 支持方法入参校验
 *
 * @author: baotingyu
 * @date: 2023/11/21
 **/
@Aspect
@Component
@Slf4j
@SuppressWarnings("all")
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class MethodParamBasedValidateAspect {

    private static final Validator DEFAULT_VALIDATOR =
            Validation.byDefaultProvider()
                    .configure()
                    .messageInterpolator(new ParameterMessageInterpolator())
                    .buildValidatorFactory()
                    .getValidator();


    @Autowired(required = false)
    private  Validator validator;

    @PostConstruct
    private void initValidator(){
        if(Objects.isNull(validator)){
            this.setValidator(DEFAULT_VALIDATOR);
        }
    }

    public void setValidator(Validator validator) {
        if(Objects.isNull(validator)){
            this.validator = DEFAULT_VALIDATOR;
            return;
        }
        this.validator = validator;
    }

    @Around("@annotation(validateMethodArgs)")
    public Object around(ProceedingJoinPoint joinPoint, ValidateMethodArgs validateMethodArgs) throws Throwable {
        if (joinPoint.getArgs().length == 0) {
            return joinPoint.proceed();
        }

        try {

            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();

            Set<String> nullMsg = new HashSet<>();
            Set<ConstraintViolation> allViolations = new HashSet<>();

            if (validateMethodArgs.groups().length == 0) {

                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (Objects.isNull(arg)) {
                        if (parameters[i].isAnnotationPresent(NotNull.class)) {
                            NotNull annotation = parameters[i].getAnnotation(NotNull.class);
                            nullMsg.add(annotation.message());
                        }
                        continue;
                    }
                    allViolations.addAll(DEFAULT_VALIDATOR.validate(arg));
                }

            } else {
                // 分组校验
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (Objects.isNull(arg)) {
                        if (parameters[i].isAnnotationPresent(NotNull.class)) {
                            NotNull annotation = parameters[i].getAnnotation(NotNull.class);
                            nullMsg.add(annotation.message());
                        }
                        continue;
                    }
                    allViolations.addAll(DEFAULT_VALIDATOR.validate(arg, validateMethodArgs.groups()));
                }
            }

            if (nullMsg.isEmpty() && allViolations.isEmpty()) {
                return joinPoint.proceed();
            }

            StringBuilder sb = new StringBuilder();
            for (String s : nullMsg) {
                sb.append(s).append(";");
            }
            for (ConstraintViolation v : allViolations) {
                sb.append(v.getMessage()).append(";");
            }
            throw new IllegalArgumentException(sb.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }


}
