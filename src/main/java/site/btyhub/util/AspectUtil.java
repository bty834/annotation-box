package site.btyhub.util;


import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Aspect工具类
 *
 * @author: baotingyu
 * @date: 2023/11/21
 **/
public class AspectUtil {
    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public static MethodBasedEvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        return new MethodBasedEvaluationContext(joinPoint.getTarget(),method,joinPoint.getArgs(),nameDiscoverer);
    }


    public static  <T> T resolveAopMethodSpringEL(ProceedingJoinPoint joinPoint,String spELString,Class<T> resultType){
        try {
            MethodBasedEvaluationContext context = buildEvaluationContext(joinPoint);
            // 构建表达式
            Expression expression = parser.parseExpression(spELString);
            // 解析
            return expression.getValue(context,resultType);
        } catch (ParseException | EvaluationException e) {
            throw new RuntimeException("SpringEL解析异常");
        }
    }
}
