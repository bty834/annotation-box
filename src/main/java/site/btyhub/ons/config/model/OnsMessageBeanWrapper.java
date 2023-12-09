package site.btyhub.ons.config.model;

import lombok.Data;
import site.btyhub.ons.annotation.OnsBatchMessageListener;
import site.btyhub.ons.annotation.OnsMessageListener;

/**
 * @author wangxinyu26
 * @date 2022/3/7 10:10 AM
 */
@Data
public class OnsMessageBeanWrapper {

    private MessageListenerTypeEnum type;

    private Object bean;

    private String beanName;

    private OnsMessageListener annotation;

    private OnsBatchMessageListener batchAnnotation;
}
