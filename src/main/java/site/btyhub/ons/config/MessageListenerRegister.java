package site.btyhub.ons.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.aliyun.openservices.ons.api.MessageListener;
import com.aliyun.openservices.ons.api.batch.BatchMessageListener;
import com.aliyun.openservices.ons.api.bean.Subscription;
import com.aliyun.openservices.ons.api.order.MessageOrderListener;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import lombok.extern.slf4j.Slf4j;
import site.btyhub.ons.annotation.OnsBatchMessageListener;
import site.btyhub.ons.annotation.OnsMessageListener;
import site.btyhub.ons.config.model.MessageListenerTypeEnum;
import site.btyhub.ons.config.model.OnsMessageBeanWrapper;
import site.btyhub.ons.constant.Constant;


@Slf4j
public class MessageListenerRegister implements SmartInitializingSingleton {

    private final ConfigurableApplicationContext context;

    private final StandardEnvironment environment;

    private final AtomicLong counter = new AtomicLong(0);

    public MessageListenerRegister(ConfigurableApplicationContext context, StandardEnvironment environment) {
        this.context = context;
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        //获取所有的 OnsBatchMessageListener OnsMessageListener
        List<OnsMessageBeanWrapper> onsMessageBeanWrapperList = findOnsMessageBeanWrappers();

        //校验，相同 nameServer-consumerGroup ， 不能存在多种 type
        validateConsumerGroupType(onsMessageBeanWrapperList);

        //按 type-nameServer-consumerGroup分组
        List<List<OnsMessageBeanWrapper>> wrapperList = onsMessageBeanWrapperList.stream()
            .collect(Collectors.groupingBy(OnsMessageBeanWrapper::getType))
            .values()
            .stream()
            .map(list -> list.stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getType().equals(MessageListenerTypeEnum.BATCH)) {
                        return item.getBatchAnnotation().nameServer().trim();
                    }
                    return item.getAnnotation().nameServer().trim();
                }))
                .values()
            )
            .flatMap(Collection::stream)
            .map(list -> list.stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getType().equals(MessageListenerTypeEnum.BATCH)) {
                        return item.getBatchAnnotation().consumerGroup().trim();
                    }
                    return item.getAnnotation().consumerGroup().trim();
                }))
                .values()
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        //校验 同一个topic下，不支持多个 listener
        validateTopic(wrapperList);

        for (List<OnsMessageBeanWrapper> onsMessageBeanWrappers : wrapperList) {
            register(onsMessageBeanWrappers);
        }
    }

    private void validateTopic(List<List<OnsMessageBeanWrapper>> wrapperList) {
        for (List<OnsMessageBeanWrapper> onsMessageBeanWrappers : wrapperList) {
            Map<String, List<OnsMessageBeanWrapper>> collect = onsMessageBeanWrappers.stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getType().equals(MessageListenerTypeEnum.BATCH)) {
                        return item.getBatchAnnotation().topic().trim();
                    }
                    return item.getAnnotation().topic().trim();
                }));
            for (List<OnsMessageBeanWrapper> messageBeanWrappers : collect.values()) {
                if (messageBeanWrappers.size() > 1){
                    throw new IllegalArgumentException(
                        "@OnsBatchMessageListener or @OnsMessageListener only be one on the same 'topic'!");
                }
            }
        }
    }

    private void validateConsumerGroupType(List<OnsMessageBeanWrapper> onsMessageBeanWrapperList) {
        List<List<OnsMessageBeanWrapper>> wrapperList = onsMessageBeanWrapperList.stream()
            .collect(Collectors.groupingBy(item -> {
                if (item.getType().equals(MessageListenerTypeEnum.BATCH)) {
                    return item.getBatchAnnotation().nameServer().trim();
                }
                return item.getAnnotation().nameServer().trim();
            }))
            .values()
            .stream()
            .map(list -> list.stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getType().equals(MessageListenerTypeEnum.BATCH)) {
                        return item.getBatchAnnotation().consumerGroup().trim();
                    }
                    return item.getAnnotation().consumerGroup().trim();
                }))
                .values()
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        for (List<OnsMessageBeanWrapper> messageBeanWrapperList : wrapperList) {
            List<MessageListenerTypeEnum> collect = messageBeanWrapperList.stream()
                .map(OnsMessageBeanWrapper::getType)
                .distinct()
                .collect(Collectors.toList());
            if (collect.size() > 1){
                throw new IllegalArgumentException(
                    "Type(eg:MessageOrderListener,MessageListener,BatchMessageListener) must be same on the same 'consumerGroup'!");
            }
        }
    }

    private List<OnsMessageBeanWrapper> findOnsMessageBeanWrappers() {
        List<OnsMessageBeanWrapper> onsMessageBeanWrapperList = findOnsSingleMessageBeanWrappers();
        List<OnsMessageBeanWrapper> onsBatchMessageBeanWrapperList = findOnsBatchMessageBeanWrappers();

        return Stream.concat(onsMessageBeanWrapperList.stream(), onsBatchMessageBeanWrapperList.stream())
            .collect(Collectors.toList());
    }

    private List<OnsMessageBeanWrapper> findOnsBatchMessageBeanWrappers() {
        Map<String, Object> beans = this.context.getBeansWithAnnotation(OnsBatchMessageListener.class)
            .entrySet()
            .stream()
            .filter(entry -> !ScopedProxyUtils.isScopedTarget(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        return beans.entrySet()
            .stream()
            .map(entry -> {
                Object bean = entry.getValue();
                Class<?> clazz = bean.getClass();
                OnsBatchMessageListener annotation = clazz.getAnnotation(OnsBatchMessageListener.class);

                OnsMessageBeanWrapper onsMessageBeanWrapper = new OnsMessageBeanWrapper();
                onsMessageBeanWrapper.setType(
                    bean instanceof BatchMessageListener ? MessageListenerTypeEnum.BATCH : null
                );
                onsMessageBeanWrapper.setBean(bean);
                onsMessageBeanWrapper.setBeanName(entry.getKey());
                onsMessageBeanWrapper.setBatchAnnotation(annotation);
                return onsMessageBeanWrapper;
            })
            .filter(item -> {
                if (item.getType() == null){
                    throw new IllegalArgumentException(
                        "Bad annotation definition in @OnsBatchMessageListener, bean must instanceof BatchMessageListener");
                }
                return canRegister(item.getBatchAnnotation());
            })
            .collect(Collectors.toList());
    }

    private List<OnsMessageBeanWrapper> findOnsSingleMessageBeanWrappers() {
        Map<String, Object> beans = this.context.getBeansWithAnnotation(OnsMessageListener.class)
            .entrySet()
            .stream()
            .filter(entry -> !ScopedProxyUtils.isScopedTarget(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return beans.entrySet()
            .stream()
            .map(entry -> {
                Object bean = entry.getValue();
                Class<?> clazz = bean.getClass();
                OnsMessageListener annotation = clazz.getAnnotation(OnsMessageListener.class);

                OnsMessageBeanWrapper onsMessageBeanWrapper = new OnsMessageBeanWrapper();
                onsMessageBeanWrapper.setType(
                    bean instanceof MessageListener ?
                        MessageListenerTypeEnum.NORMAL :
                        (bean instanceof MessageOrderListener? MessageListenerTypeEnum.ORDER : null)
                );
                onsMessageBeanWrapper.setBean(bean);
                onsMessageBeanWrapper.setBeanName(entry.getKey());
                onsMessageBeanWrapper.setAnnotation(annotation);
                return onsMessageBeanWrapper;
            })
            .filter(item -> {
                if (item.getType() == null){
                    throw new IllegalArgumentException(
                        "Bad annotation definition in @OnsMessageListener, bean must instanceof MessageOrderListener or MessageListener");
                }
                if (MessageListenerTypeEnum.ORDER.equals(item.getType()) && item.getAnnotation().messageModel() == MessageModel.BROADCASTING) {
                    throw new IllegalArgumentException(
                        "Bad annotation definition in @OnsMessageListener, messageModel BROADCASTING does not support ORDERLY message!");
                }
                return canRegister(item.getAnnotation());
            })
            .collect(Collectors.toList());
    }

    private void register(List<OnsMessageBeanWrapper> onsMessageBeanWrapperList) {
        if (CollectionUtils.isEmpty(onsMessageBeanWrapperList)){
            return;
        }

        OnsMessageBeanWrapper onsMessageBeanWrapper;
        if (onsMessageBeanWrapperList.size() > 1){
            List<OnsMessageBeanWrapper> collect = onsMessageBeanWrapperList.stream()
                .filter(item -> {
                    if (item.getType() == MessageListenerTypeEnum.BATCH){
                        return item.getBatchAnnotation().primary();
                    }
                    return item.getAnnotation().primary();
                })
                .collect(Collectors.toList());
            if (collect.size() != 1){
                throw new IllegalArgumentException("Property 'primary' must be only when multi @OnsMessageListener exist on the same 'consumerGroup'!");
            }
            onsMessageBeanWrapper = collect.get(0);
        }else{
            onsMessageBeanWrapper = onsMessageBeanWrapperList.get(0);
        }

        String containerBeanName = String.format("%s_%s", onsMessageBeanWrapper.getBeanName(), counter.incrementAndGet());
        GenericApplicationContext genericApplicationContext = (GenericApplicationContext) context;

        GenericBeanDefinition containerBeanDefinition;
        if (onsMessageBeanWrapper.getType() == MessageListenerTypeEnum.NORMAL){
            containerBeanDefinition = createOnsMessageListenerContainerBeanDefinition(onsMessageBeanWrapper.getAnnotation(), onsMessageBeanWrapperList);
        }else if (onsMessageBeanWrapper.getType() == MessageListenerTypeEnum.ORDER){
            containerBeanDefinition = createOnsMessageOrderListenerContainerBeanDefinition(onsMessageBeanWrapper.getAnnotation(), onsMessageBeanWrapperList);
        }else{
            containerBeanDefinition = createOnsBatchMessageListenerContainerBeanDefinition(onsMessageBeanWrapper.getBatchAnnotation(), onsMessageBeanWrapperList);
        }
        genericApplicationContext.registerBeanDefinition(containerBeanName, containerBeanDefinition);
        Lifecycle container = (Lifecycle) genericApplicationContext.getBean(containerBeanName);
        if (!container.isRunning()) {
            try {
                container.start();
            } catch (Exception e) {
                log.error(Constant.PREFIX + "Started container failed. {}", container, e);
                throw new RuntimeException(e);
            }
        }

        List<String> listenerBeanNameList = onsMessageBeanWrapperList.stream()
            .map(OnsMessageBeanWrapper::getBeanName)
            .collect(Collectors.toList());
        log.info(Constant.PREFIX + "Register the listener to container, listenerBeanName:{}, containerBeanName:{}", listenerBeanNameList, containerBeanName);
    }

    private GenericBeanDefinition createOnsMessageListenerContainerBeanDefinition(OnsMessageListener annotation, List<OnsMessageBeanWrapper> onsMessageBeanWrapperList) {

        String nameServer = environment.resolvePlaceholders(annotation.nameServer().trim());
        String accessKey = environment.resolvePlaceholders(annotation.accessKey().trim());
        String secretKey = environment.resolvePlaceholders(annotation.secretKey().trim());
        String consumerGroup = environment.resolvePlaceholders(annotation.consumerGroup().trim());
        String consumeThreadNums = environment.resolvePlaceholders(annotation.consumeThreadNums().trim());
        String maxReconsumeTimes = environment.resolvePlaceholders(annotation.maxReconsumeTimes().trim());
        String consumeTimeout = environment.resolvePlaceholders(annotation.consumeTimeout().trim());

        Map<Subscription, MessageListener> subscriptionTable = onsMessageBeanWrapperList.stream()
            .collect(
                Collectors.toMap(
                    item -> {
                        String topic = environment.resolvePlaceholders(item.getAnnotation().topic().trim());
                        String tag = environment.resolvePlaceholders(item.getAnnotation().tag().trim());
                        return buildSubscription(topic, tag);
                    },
                    item -> (MessageListener)item.getBean(),
                    (k1, k2) -> k2)
            );

        ManagedList<Object> managedList = onsMessageBeanWrapperList.stream()
            .map(OnsMessageBeanWrapper::getBeanName)
            //塞入 BeanDefinition 也可以
            .map(RuntimeBeanReference::new)
            .collect(ManagedList::new, ManagedList::add, ManagedList::addAll);

        GenericBeanDefinition rootBeanDefinition = new GenericBeanDefinition();
        rootBeanDefinition.setBeanClass(OnsMessageListenerContainer.class);

        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.add("accessKey", accessKey);
        mutablePropertyValues.add("secretKey", secretKey);
        mutablePropertyValues.add("nameServer", nameServer);
        mutablePropertyValues.add("consumerGroup", consumerGroup);
        mutablePropertyValues.add("messageModel", annotation.messageModel());
        mutablePropertyValues.add("consumeThreadNums", consumeThreadNums);
        mutablePropertyValues.add("maxReconsumeTimes", maxReconsumeTimes);
        mutablePropertyValues.add("consumeTimeout", consumeTimeout);
        mutablePropertyValues.add("subscriptionTable", subscriptionTable);
        //做spring依赖传递
        mutablePropertyValues.add("messageListenerList", managedList);

        rootBeanDefinition.setPropertyValues(mutablePropertyValues);
        return rootBeanDefinition;
    }

    private GenericBeanDefinition createOnsMessageOrderListenerContainerBeanDefinition(OnsMessageListener annotation, List<OnsMessageBeanWrapper> onsMessageBeanWrapperList) {

        String nameServer = environment.resolvePlaceholders(annotation.nameServer().trim());
        String accessKey = environment.resolvePlaceholders(annotation.accessKey().trim());
        String secretKey = environment.resolvePlaceholders(annotation.secretKey().trim());
        String consumerGroup = environment.resolvePlaceholders(annotation.consumerGroup().trim());
        String consumeThreadNums = environment.resolvePlaceholders(annotation.consumeThreadNums().trim());
        String maxReconsumeTimes = environment.resolvePlaceholders(annotation.maxReconsumeTimes().trim());
        String consumeTimeout = environment.resolvePlaceholders(annotation.consumeTimeout().trim());
        String suspendTimeMillis = environment.resolvePlaceholders(annotation.suspendTimeMillis().trim());
        String enableOrderlyConsumeAccelerator = environment.resolvePlaceholders(annotation.enableOrderlyConsumeAccelerator().trim());

        Map<Subscription, MessageOrderListener> subscriptionTable = onsMessageBeanWrapperList.stream()
            .collect(
                Collectors.toMap(
                    item -> {
                        String topic = environment.resolvePlaceholders(item.getAnnotation().topic().trim());
                        String tag = environment.resolvePlaceholders(item.getAnnotation().tag().trim());
                        return buildSubscription(topic, tag);
                    },
                    item -> (MessageOrderListener)item.getBean(),
                    (k1, k2) -> k2)
            );

        ManagedList<Object> managedList = onsMessageBeanWrapperList.stream()
            .map(OnsMessageBeanWrapper::getBeanName)
            //塞入 BeanDefinition 也可以
            .map(RuntimeBeanReference::new)
            .collect(ManagedList::new, ManagedList::add, ManagedList::addAll);

        GenericBeanDefinition rootBeanDefinition = new GenericBeanDefinition();
        rootBeanDefinition.setBeanClass(OnsMessageOrderListenerContainer.class);

        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.add("accessKey", accessKey);
        mutablePropertyValues.add("secretKey", secretKey);
        mutablePropertyValues.add("nameServer", nameServer);
        mutablePropertyValues.add("consumerGroup", consumerGroup);
        mutablePropertyValues.add("messageModel", annotation.messageModel());
        mutablePropertyValues.add("consumeThreadNums", consumeThreadNums);
        mutablePropertyValues.add("maxReconsumeTimes", maxReconsumeTimes);
        mutablePropertyValues.add("consumeTimeout", consumeTimeout);
        mutablePropertyValues.add("suspendTimeMillis", suspendTimeMillis);
        mutablePropertyValues.add("enableOrderlyConsumeAccelerator", enableOrderlyConsumeAccelerator);
        mutablePropertyValues.add("subscriptionTable", subscriptionTable);
        //做spring依赖传递
        mutablePropertyValues.add("messageListenerList", managedList);

        rootBeanDefinition.setPropertyValues(mutablePropertyValues);
        return rootBeanDefinition;
    }

    private GenericBeanDefinition createOnsBatchMessageListenerContainerBeanDefinition(OnsBatchMessageListener annotation, List<OnsMessageBeanWrapper> onsMessageBeanWrapperList) {

        String accessKey = environment.resolvePlaceholders(annotation.accessKey().trim());
        String secretKey = environment.resolvePlaceholders(annotation.secretKey().trim());
        String nameServer = environment.resolvePlaceholders(annotation.nameServer().trim());
        String consumerGroup = environment.resolvePlaceholders(annotation.consumerGroup().trim());
        String consumeThreadNums = environment.resolvePlaceholders(annotation.consumeThreadNums().trim());
        String maxReconsumeTimes = environment.resolvePlaceholders(annotation.maxReconsumeTimes().trim());
        String consumeTimeout = environment.resolvePlaceholders(annotation.consumeTimeout().trim());
        String consumeMessageBatchMaxSize = environment.resolvePlaceholders(annotation.consumeMessageBatchMaxSize().trim());
        String batchConsumeMaxAwaitDurationInSeconds = environment.resolvePlaceholders(annotation.batchConsumeMaxAwaitDurationInSeconds().trim());

        Map<Subscription, BatchMessageListener> subscriptionTable = onsMessageBeanWrapperList.stream()
            .collect(
                Collectors.toMap(
                    item -> {
                        String topic = environment.resolvePlaceholders(item.getBatchAnnotation().topic().trim());
                        String tag = environment.resolvePlaceholders(item.getBatchAnnotation().tag().trim());
                        return buildSubscription(topic, tag);
                    },
                    item -> (BatchMessageListener)item.getBean(),
                    (k1, k2) -> k2)
            );

        ManagedList<Object> managedList = onsMessageBeanWrapperList.stream()
            .map(OnsMessageBeanWrapper::getBeanName)
            //塞入 BeanDefinition 也可以
            .map(RuntimeBeanReference::new)
            .collect(ManagedList::new, ManagedList::add, ManagedList::addAll);

        GenericBeanDefinition rootBeanDefinition = new GenericBeanDefinition();
        rootBeanDefinition.setBeanClass(OnsBatchMessageListenerContainer.class);

        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
        mutablePropertyValues.add("accessKey", accessKey);
        mutablePropertyValues.add("secretKey", secretKey);
        mutablePropertyValues.add("nameServer", nameServer);
        mutablePropertyValues.add("consumerGroup", consumerGroup);
        mutablePropertyValues.add("messageModel", annotation.messageModel());
        mutablePropertyValues.add("consumeThreadNums", consumeThreadNums);
        mutablePropertyValues.add("maxReconsumeTimes", maxReconsumeTimes);
        mutablePropertyValues.add("consumeTimeout", consumeTimeout);
        mutablePropertyValues.add("consumeMessageBatchMaxSize", consumeMessageBatchMaxSize);
        mutablePropertyValues.add("batchConsumeMaxAwaitDurationInSeconds", batchConsumeMaxAwaitDurationInSeconds);
        mutablePropertyValues.add("subscriptionTable", subscriptionTable);
        //做spring依赖传递
        mutablePropertyValues.add("messageListenerList", managedList);

        rootBeanDefinition.setPropertyValues(mutablePropertyValues);
        return rootBeanDefinition;
    }

    private Subscription buildSubscription(String topic, String tag) {
        Subscription subscription = new Subscription();
        subscription.setTopic(topic);
        // 绑定要监听的tag，多个tag用 || 隔开
        subscription.setExpression(tag);
        return subscription;
    }

    private boolean canRegister(OnsBatchMessageListener annotation) {
        if (!isRemoteEnv()) {
            return annotation.localRegister();
        }
        return true;
    }

    private boolean canRegister(OnsMessageListener annotation) {
        if (!isRemoteEnv()) {
            return annotation.localRegister();
        }
        return true;
    }

    private boolean isRemoteEnv() {
        String property = environment.getProperty("os.name");
        return !ObjectUtils.isEmpty(property) && property.toLowerCase().contains("linux");
    }
}
