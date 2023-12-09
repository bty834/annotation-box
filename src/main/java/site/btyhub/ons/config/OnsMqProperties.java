package site.btyhub.ons.config;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * ons配置参数<br/>
 * 参考：https://help.aliyun.com/document_detail/93574.html?spm=a2c4g.11186623.6.553.927d650eeh6vzK <br/>
 * <p>
 * - AccessKey	                String	-	        您在阿里云账号管理控制台中创建的 AccessKeyId，用于身份认证。<br/>
 * - SecretKey	                String	-	        您在阿里云账号管理控制台中创建的 AccessKeySecret，用于身份认证。<br/>
 * - OnsChannel	                String	ALIYUN	    用户渠道，阿里云：ALIYUN，聚石塔用户为：CLOUD。<br/>
 * - NAMESRV_ADDR	                String	-	        设置 TCP 协议接入点。<br/>
 * - GROUP_ID	                    String	-	        Consumer 实例的唯一 ID，您在控制台创建的 Group ID。<br/>
 * - MessageModel	                String	CLUSTERING	设置 Consumer 实例的消费模式，集群消费：CLUSTERING，广播消费：BROADCASTING。<br/>
 * - ConsumeThreadNums	        String	64	        消费线程数量。<br/>
 * - MaxReconsumeTimes	        String	16	        设置消息消费失败的最大重试次数。<br/>
 * - ConsumeTimeout	            String	15	        设置每条消息消费的最大超时时间，超过设置时间则被视为消费失败，等下次重新投递再次消费。每个业务需要设置一个合理的值，单位：分钟（min）。<br/>
 * - ConsumeMessageBatchMaxSize	String	1	        BatchConsumer每次批量消费的最大消息数量，默认值为1，允许自定义范围为[1, 32]，实际消费数量可能小于该值。<br/>
 * - CheckImmunityTimeInSeconds	String	30	        设置事务消息第一次回查的最快时间，单位：秒（s）。<br/>
 * - suspendTimeMillis	        String	3000        只适用于顺序消息，设置消息消费失败的重试间隔时间，单位：毫秒（ms）。<br/>
 * </p>
 */
public class OnsMqProperties implements Serializable {

    private String nameServer;

    private String accessKey;
    private String secretKey;

    private Producer producer;

    private Consumer consumer = new Consumer();

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
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

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public static class Producer {

        /**
         * Group name of producer.
         */
        private String group;

        /**
         * Namespace for this MQ Producer instance.
         */
        private String namespace;

        /**
         * Millis of send message timeout.
         */
        private int sendMessageTimeout = 3000;

        /**
         * Compress message body threshold, namely, message body larger than 4k will be compressed on default.
         */
        private int compressMessageBodyThreshold = 1024 * 4;

        /**
         * Maximum number of retry to perform internally before claiming sending failure in synchronous mode.
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * <p> Maximum number of retry to perform internally before claiming sending failure in asynchronous mode. </p>
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * Indicate whether to retry another broker on sending failure internally.
         */
        private boolean retryNextServer = false;

        /**
         * Maximum allowed message size in bytes.
         */
        private int maxMessageSize = 1024 * 1024 * 4;

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;


        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public int getSendMessageTimeout() {
            return sendMessageTimeout;
        }

        public void setSendMessageTimeout(int sendMessageTimeout) {
            this.sendMessageTimeout = sendMessageTimeout;
        }

        public int getCompressMessageBodyThreshold() {
            return compressMessageBodyThreshold;
        }

        public void setCompressMessageBodyThreshold(int compressMessageBodyThreshold) {
            this.compressMessageBodyThreshold = compressMessageBodyThreshold;
        }

        public int getRetryTimesWhenSendFailed() {
            return retryTimesWhenSendFailed;
        }

        public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
            this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
        }

        public int getRetryTimesWhenSendAsyncFailed() {
            return retryTimesWhenSendAsyncFailed;
        }

        public void setRetryTimesWhenSendAsyncFailed(int retryTimesWhenSendAsyncFailed) {
            this.retryTimesWhenSendAsyncFailed = retryTimesWhenSendAsyncFailed;
        }

        public boolean isRetryNextServer() {
            return retryNextServer;
        }

        public void setRetryNextServer(boolean retryNextServer) {
            this.retryNextServer = retryNextServer;
        }

        public int getMaxMessageSize() {
            return maxMessageSize;
        }

        public void setMaxMessageSize(int maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
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

        public boolean isEnableMsgTrace() {
            return enableMsgTrace;
        }

        public void setEnableMsgTrace(boolean enableMsgTrace) {
            this.enableMsgTrace = enableMsgTrace;
        }


        public boolean isTlsEnable() {
            return tlsEnable;
        }

        public void setTlsEnable(boolean tlsEnable) {
            this.tlsEnable = tlsEnable;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }


    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public static final class Consumer {
        /**
         * Group name of consumer.
         */
        private String group;

        /**
         * Namespace for this MQ Consumer instance.
         */
        private String namespace;

        /**
         * Topic name of consumer.
         */
        private String topic;

        /**
         * Control message mode, if you want all subscribers receive message all message, broadcasting is a good choice.
         */
        private String messageModel = "CLUSTERING";

        /**
         * Control how to selector message.
         */
        private String selectorType = "TAG";

        /**
         * Control which message can be select.
         */
        private String selectorExpression = "*";

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Maximum number of messages pulled each time.
         */
        private int pullBatchSize = 10;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;


        /**
         * listener configuration container
         * the pattern is like this:
         * group1.topic1 = false
         * group2.topic2 = true
         * group3.topic3 = false
         */
        private Map<String, Map<String, Boolean>> listeners = new HashMap<>();

        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getMessageModel() {
            return messageModel;
        }

        public void setMessageModel(String messageModel) {
            this.messageModel = messageModel;
        }

        public String getSelectorType() {
            return selectorType;
        }

        public void setSelectorType(String selectorType) {
            this.selectorType = selectorType;
        }

        public String getSelectorExpression() {
            return selectorExpression;
        }

        public void setSelectorExpression(String selectorExpression) {
            this.selectorExpression = selectorExpression;
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

        public int getPullBatchSize() {
            return pullBatchSize;
        }

        public void setPullBatchSize(int pullBatchSize) {
            this.pullBatchSize = pullBatchSize;
        }

        public Map<String, Map<String, Boolean>> getListeners() {
            return listeners;
        }

        public void setListeners(Map<String, Map<String, Boolean>> listeners) {
            this.listeners = listeners;
        }

        public boolean isEnableMsgTrace() {
            return enableMsgTrace;
        }

        public void setEnableMsgTrace(boolean enableMsgTrace) {
            this.enableMsgTrace = enableMsgTrace;
        }


        public boolean isTlsEnable() {
            return tlsEnable;
        }

        public void setTlsEnable(boolean tlsEnable) {
            this.tlsEnable = tlsEnable;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }
}
