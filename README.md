`@OnsMessageListener`注册mq消费者：
```java
package com.gaotu.lvyue.fulfillment.server.rockermq.consumer.fulfillment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;
import com.aliyun.openservices.ons.api.order.MessageOrderListener;
import com.aliyun.openservices.ons.api.order.OrderAction;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSONObject;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.gaotu.arch.ons.annotation.OnsMessageListener;
import com.gaotu.lvyue.fulfillment.domain.platform.exception.FulfillmentBizException;
import com.gaotu.lvyue.fulfillment.server.rockermq.consumer.fulfillment.other.IFulfillmentItemConsumeProcessor;
import lombok.extern.slf4j.Slf4j;

@Component
@OnsMessageListener(
        consumerGroup = "${fulfillment.platform.item.other.groupId:GID_test}",
        topic = "${fulfillment.platform.item.other.topic:topic}",
        tag = "${fulfillment.platform.item.other.tag:*}",
        nameServer = "${ons.mq.nameServer: http://nameServer.com:80}",
        accessKey = "${aliyun.key.access-key:access-key}",
        secretKey = "${aliyun.key.secret-key:secret-key}", localRegister = true)
@Slf4j
@SuppressWarnings("all")
public class FulfillmentItemEventConsumer implements MessageOrderListener {

    @Autowired
    private Map<String, IFulfillmentItemConsumeProcessor> fulfillmentItemConsumerMap;

    @Value("${fulfillment.item.other.skip.tags:[\"fulfillmentCredentialDispatchEvent\",\"fulfillmentProgressUpdateEvent\"]}")
    private List<String> skipTags;
    @ApolloJsonValue("${fulfillment.item.other.msg.consumer.filter:[]}")
    private Set<String> messageFilter;
    @Value("${fulfillment.item.other.maxRetry:3}")
    private Integer fullfillmentOrderMaxRetry;
    @Value("${fulfillment.item.other.stop.consumer.switch:false}")
    private boolean stopConsumerSwitch;
    @Value("${fulfillment.item.other.force.stop.consumer.switch:false}")
    private boolean forceStopConsumerSwitch;
    @Value("${fulfillment.item.other.stop.consumer.error.switch:false}")
    private boolean stopConsumerErrorReportSwitch;
    @Override
    public OrderAction consume(Message message, ConsumeOrderContext context) {
        String msgID = message.getMsgID();
        try {
            MDC.put("logger_id", msgID);
            log.info("履约服务获取消息参数为{}", JSONObject.toJSONString(message));
            if (stopConsumerSwitch) {
                return OrderAction.Suspend;
            }
            //过滤消息
            if (CollectionUtils.isNotEmpty(messageFilter) && messageFilter.contains(msgID)) {
                return OrderAction.Success;
            }
            String tag = message.getTag();
            //
            if(skipTags.contains(tag)){
                return OrderAction.Success;
            }

            String key = tag + IFulfillmentItemConsumeProcessor.CONSUMER_SUFFIX;
            IFulfillmentItemConsumeProcessor itemConsumer = fulfillmentItemConsumerMap.get(key);

            if (Objects.isNull(itemConsumer)) {
                log.error("履约服务获取消息处理器失败,处理器{}", key);
                return OrderAction.Success;
            }
            boolean consumer = itemConsumer.consumer(message);
            if (consumer) {
                return OrderAction.Success;
            }
            return OrderAction.Suspend;
        } catch (FulfillmentBizException bizException){
            int reconsumeTimes = message.getReconsumeTimes();
            if (reconsumeTimes > fullfillmentOrderMaxRetry) {
                log.error("履约业务异常，消费失败，消息重试达到最大值", bizException);
                return OrderAction.Success;
            }
            log.error("履约业务异常，消费失败，重试中",bizException);
            return OrderAction.Suspend;
        } catch (Exception e) {
            int reconsumeTimes = message.getReconsumeTimes();
            if (reconsumeTimes > fullfillmentOrderMaxRetry) {
                if (stopConsumerErrorReportSwitch) {
                    log.warn("履约服务消费消息达到最大重试次数3次", e);
                } else {
                    log.error("履约服务消费消息达到最大重试次数3次", e);
                }
                if (forceStopConsumerSwitch) {
                    log.info("履约服务消费消息达到最大重试次数直接消费成功{}", message.getMsgID());
                    return OrderAction.Success;
                }
            } else {
                log.warn("履约服务消费消息失败", e);
            }
            return OrderAction.Suspend;
        } finally {
            MDC.clear();
        }
    }
}
```
`@ValidateMethodArgs` 和 `@DistributedLock` :
```java
@Component
@Slf4j
public class FulfillmentItemHandler {
    // ...
    @ValidateMethodArgs
    @DistributedLock(
            name = "'" + Constants.FULFILLMENT_ORDER_LOCK_PREFIX + "'+#itemsPerOrder.fulfillmentOrderNumber",
            timeoutPropertyKey = "lock.fulfillment.order.timeout.ms",
            exceptionMsg = "创建履约项异常")
    public void itemsReport(@NotNull FulfillmentItemReportDTO itemsPerOrder) {
        // ...
    }
    // ...
}

@Data
@ToString
public class FulfillmentItemReportDTO {

    @NotNull
    private Long fulfillmentOrderNumber;
    
    @NotNull
    @Valid
    private List<FulfillmentItemReportInfoDTO> items;
}

```

