package site.btyhub.ons.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.batch.BatchMessageListener;
import com.aliyun.openservices.ons.api.bean.BatchConsumerBean;
import com.aliyun.openservices.ons.api.bean.Subscription;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import lombok.extern.slf4j.Slf4j;
import site.btyhub.ons.constant.GaotuOnsConstant;


@Slf4j
@SuppressWarnings("all")
public class OnsBatchMessageListenerContainer implements InitializingBean, DisposableBean, OnsListenerContainer, SmartLifecycle {

    private String accessKey;

    private String secretKey;

    private String nameServer;

    private String consumerGroup;

    private MessageModel messageModel;

    private String consumeThreadNums;

    private String maxReconsumeTimes;

    private String consumeTimeout;

    private String consumeMessageBatchMaxSize;

    private String batchConsumeMaxAwaitDurationInSeconds;

    private List<BatchMessageListener> messageListenerList;

    private Map<Subscription, BatchMessageListener> subscriptionTable;

    private BatchConsumerBean consumerBean;

    private boolean running;

    @Override
    public void afterPropertiesSet() throws Exception {
        initRocketMQPushConsumer();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        // Returning Integer.MAX_VALUE only suggests that
        // we will be the first bean to shutdown and last bean to start
        return Integer.MAX_VALUE;
    }

    @Override
    public void start() {
        if (this.isRunning()) {
            throw new IllegalStateException("container already running. " + this.toString());
        }

        try {
            if (Objects.nonNull(consumerBean)) {
                consumerBean.start();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start RocketMQ push consumer", e);
        }
        running = true;

        log.info(GaotuOnsConstant.SYMBOL + "running batch consumer container: {}", this.toString());
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void stop() {
        if (this.isRunning()) {
            if (Objects.nonNull(consumerBean)) {
                consumerBean.shutdown();
            }
            running = false;
        }
    }

    @Override
    public void destroy() throws Exception {
        running = false;
        if (Objects.nonNull(consumerBean)) {
            consumerBean.shutdown();
        }
        log.info(GaotuOnsConstant.SYMBOL + " container destroyed, {}", this.toString());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void initRocketMQPushConsumer() {
        Assert.notNull(consumerGroup, "Property 'consumerGroup' is required");
        Assert.notNull(nameServer, "Property 'nameServer' is required");
        Assert.isTrue(!CollectionUtils.isEmpty(subscriptionTable), "Property 'subscriptionTable' is required");

        Properties properties = commonProperties();
        consumerBean = new BatchConsumerBean();
        consumerBean.setProperties(properties);
        consumerBean.setSubscriptionTable(subscriptionTable);
    }


    private Properties commonProperties() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, accessKey);
        properties.put(PropertyKeyConst.SecretKey, secretKey);
        properties.put(PropertyKeyConst.NAMESRV_ADDR, nameServer);
        properties.put(PropertyKeyConst.GROUP_ID, consumerGroup);
        properties.put(PropertyKeyConst.MessageModel, messageModel);

        if (!StringUtils.isEmpty(consumeThreadNums)){
            properties.put(PropertyKeyConst.ConsumeThreadNums, consumeThreadNums);
        }
        if (!StringUtils.isEmpty(maxReconsumeTimes)){
            properties.put(PropertyKeyConst.MaxReconsumeTimes, maxReconsumeTimes);
        }
        if (!StringUtils.isEmpty(consumeTimeout)){
            properties.put(PropertyKeyConst.ConsumeTimeout, consumeTimeout);
        }
        if (!StringUtils.isEmpty(consumeMessageBatchMaxSize)){
            properties.put(PropertyKeyConst.ConsumeMessageBatchMaxSize, consumeMessageBatchMaxSize);
        }
        if (!StringUtils.isEmpty(batchConsumeMaxAwaitDurationInSeconds)){
            properties.put(PropertyKeyConst.BatchConsumeMaxAwaitDurationInSeconds, batchConsumeMaxAwaitDurationInSeconds);
        }
        return properties;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public MessageModel getMessageModel() {
        return messageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public String getConsumeThreadNums() {
        return consumeThreadNums;
    }

    public void setConsumeThreadNums(String consumeThreadNums) {
        this.consumeThreadNums = consumeThreadNums;
    }

    public String getMaxReconsumeTimes() {
        return maxReconsumeTimes;
    }

    public void setMaxReconsumeTimes(String maxReconsumeTimes) {
        this.maxReconsumeTimes = maxReconsumeTimes;
    }

    public String getConsumeTimeout() {
        return consumeTimeout;
    }

    public void setConsumeTimeout(String consumeTimeout) {
        this.consumeTimeout = consumeTimeout;
    }

    public String getConsumeMessageBatchMaxSize() {
        return consumeMessageBatchMaxSize;
    }

    public void setConsumeMessageBatchMaxSize(String consumeMessageBatchMaxSize) {
        this.consumeMessageBatchMaxSize = consumeMessageBatchMaxSize;
    }

    public String getBatchConsumeMaxAwaitDurationInSeconds() {
        return batchConsumeMaxAwaitDurationInSeconds;
    }

    public void setBatchConsumeMaxAwaitDurationInSeconds(String batchConsumeMaxAwaitDurationInSeconds) {
        this.batchConsumeMaxAwaitDurationInSeconds = batchConsumeMaxAwaitDurationInSeconds;
    }

    public List<BatchMessageListener> getMessageListenerList() {
        return messageListenerList;
    }

    public void setMessageListenerList(List<BatchMessageListener> messageListenerList) {
        this.messageListenerList = messageListenerList;
    }

    public Map<Subscription, BatchMessageListener> getSubscriptionTable() {
        return subscriptionTable;
    }

    public void setSubscriptionTable(Map<Subscription, BatchMessageListener> subscriptionTable) {
        this.subscriptionTable = subscriptionTable;
    }
}
