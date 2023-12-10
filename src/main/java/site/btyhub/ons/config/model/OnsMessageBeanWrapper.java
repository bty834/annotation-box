package site.btyhub.ons.config.model;

import lombok.Data;
import site.btyhub.ons.annotation.OnsBatchMessageListener;
import site.btyhub.ons.annotation.OnsMessageListener;


@Data
public class OnsMessageBeanWrapper {

    private MessageListenerTypeEnum type;

    private Object bean;

    private String beanName;

    private OnsMessageListener annotation;

    private OnsBatchMessageListener batchAnnotation;
}
