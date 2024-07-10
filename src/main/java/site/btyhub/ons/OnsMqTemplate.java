package site.btyhub.ons;

import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.common.RemotingHelper;

import site.btyhub.ons.constant.Constant;


public class OnsMqTemplate implements InitializingBean, DisposableBean, OnsMqOperations {

    private static final Logger log = LoggerFactory.getLogger(OnsMqTemplate.class);

    private OnsMqTemplateProperties onsMqTemplateProperties;

    private Producer producer;

    /**
     * 顺序消息、定时消息、事务消息
     */
    private OrderProducer orderProducer;

    @Override
    public SendResult sendOrderMsg(String topic, String tags, String body, String shardingKey) {
        if (!orderProducer.isStarted()) {
            log.warn(Constant.PREFIX + "Send mq message failed.The orderProducer not started!");
            throw new RuntimeException(Constant.PREFIX + "Send mq message failed.The orderProducer not started!");
        }
        SendResult sendResult = new SendResult();
        Message message = buildMessage(topic, tags, "", body);
        try {
            sendResult = orderProducer.send(message, shardingKey);
            if (sendResult != null) {
                log.info(Constant.PREFIX +
                    " Send order message success. Topic is:"
                        + message.getTopic()
                        + " msgId is: "
                        + sendResult.getMessageId());
            }

        } catch (Exception e) {
            log.error(Constant.PREFIX + " Send order message failed. Topic is:" + message.getTopic());
            e.printStackTrace();
        }
        return sendResult;
    }

    @Override
    public SendResult send(String topic, String tags, String body) {
        return send(topic, tags, "", body);
    }

    private SendResult send(String topic, String tags, String keys, String body) {
        Message message = buildMessage(topic, tags, keys, body);
        return sendMsg(message);
    }

    @Override
    public boolean sendOneWayMes(String topic, String tags, String body) {
        Message message = buildMessage(topic, tags, "", body);
        try {
            // 由于在 oneway
            // 方式发送消息时没有请求应答处理，一旦出现消息发送失败，则会因为没有重试而导致数据丢失。若数据不可丢，建议选用可靠同步或可靠异步发送方式。
            producer.sendOneway(message);
            log.info(Constant.PREFIX + " Send mq message success. Topic is:" + message.getTopic());
            return true;
        } catch (Exception e) {
            log.error(Constant.PREFIX +
                    " Send mq message failed. Topic is: {}, msgId: {}, error : {}",
                message.getTopic(),
                message.getMsgID(),
                e.getMessage());
            return false;
        }
    }

    /**
     * 单向发送
     *
     * @param message .
     * @return .
     */
    @Override
    public boolean sendOneWayMes(Message message) {
        try {
            // 由于在 oneway
            // 方式发送消息时没有请求应答处理，一旦出现消息发送失败，则会因为没有重试而导致数据丢失。若数据不可丢，建议选用可靠同步或可靠异步发送方式。
            producer.sendOneway(message);
            log.info(Constant.PREFIX + " Send mq message success. Topic is:" + message.getTopic());
            return true;
        } catch (Exception e) {
            log.error(Constant.PREFIX +
                    " Send mq message failed. Topic is: {}, msgId: {}, error : {}",
                message.getTopic(),
                message.getMsgID(),
                e.getMessage());
            return false;
        }
    }

    @Override
    public SendResult sendOrderMsg(Message message, String shardingKey) {
        if (!orderProducer.isStarted()) {
            log.warn(Constant.PREFIX + "Send mq message failed.The orderProducer not started!");
            throw new RuntimeException("Send mq message failed.The orderProducer not started!");
        }
        SendResult sendResult = new SendResult();
        try {
            sendResult = orderProducer.send(message, shardingKey);
            if (sendResult != null) {
                log.info(Constant.PREFIX +
                    " Send order message success. Topic is:"
                        + message.getTopic()
                        + " msgId is: "
                        + sendResult.getMessageId());
            }

        } catch (Exception e) {
            log.error(Constant.PREFIX + " Send order message failed. Topic is:" + message.getTopic());
            e.printStackTrace();
        }
        return sendResult;
    }

    @Override
    public SendResult sendMsg(Message message) {
        if (!producer.isStarted()) {
            log.warn(Constant.PREFIX + "Send mq message failed.The producer not started!");
            throw new RuntimeException("Send mq message failed.The producer not started!");
        }

        SendResult sendResult = new SendResult();
        try {
            sendResult = producer.send(message);
            if (sendResult != null) {
                log.info(Constant.PREFIX +
                    " Send mq message success. Topic is:"
                        + message.getTopic()
                        + " msgId is: "
                        + sendResult.getMessageId());
            }

        } catch (Exception e) {
            log.error(Constant.PREFIX + " Send mq message failed. Topic is:" + message.getTopic());
            e.printStackTrace();
        }
        return sendResult;
    }

    private Message buildMessage(String topic, String tags, String keys, String body) {
        Message message = new Message();
        message.setTopic(topic);
        message.setTag(tags);
        message.setKey(keys);
        try {
            message.setBody(body.getBytes(RemotingHelper.DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            log.error(Constant.PREFIX + "send topic failed", e);
        }
        return message;
    }

    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }

        if (Objects.nonNull(orderProducer)) {
            orderProducer.shutdown();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Properties properties = this.commonProperties(onsMqTemplateProperties);
        producer = ONSFactory.createProducer(properties);
        producer.start();
        orderProducer = ONSFactory.createOrderProducer(properties);
        orderProducer.start();
    }

    private Properties commonProperties(OnsMqTemplateProperties onsMqTemplateProperties) {
        Properties properties = new Properties();
        // AccessKey 阿里云身份验证，在阿里云服务器管理控制台创建
        properties.put(PropertyKeyConst.AccessKey, onsMqTemplateProperties.getAccessKey());
        // SecretKey 阿里云身份验证，在阿里云服务器管理控制台创建
        properties.put(PropertyKeyConst.SecretKey, onsMqTemplateProperties.getSecretKey());
        // 设置 TCP 接入域名（此处以公共云生产环境为例）
        properties.put(PropertyKeyConst.NAMESRV_ADDR, onsMqTemplateProperties.getNameServer());
        properties.put(PropertyKeyConst.GROUP_ID, onsMqTemplateProperties.getGroup());

        // 设置发送超时时间，单位毫秒
        properties.setProperty(
            PropertyKeyConst.SendMsgTimeoutMillis,
            String.valueOf(onsMqTemplateProperties.getSendMessageTimeout()));
        return properties;
    }

    public void setOnsMqTemplateProperties(OnsMqTemplateProperties onsMqTemplateProperties) {
        this.onsMqTemplateProperties = onsMqTemplateProperties;
    }
}
