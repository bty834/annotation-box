package site.btyhub.ons.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;



@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface OnsBatchMessageListener {

    /**
     * AccessKey, 用于标识、校验用户身份
     */
    String accessKey();

    /**
     * SecretKey, 用于标识、校验用户身份
     */
    String secretKey();

    /**
     * Name Server地址
     */
    String nameServer();

    /**
     * Group ID，客户端ID
     */
    String consumerGroup();

    /**
     * Topic
     */
    String topic();

    /**
     * tag 格式
     * "tag1 || tag2 || tag3", <br>
     * null 或 * 意味着订阅所有
     */
    String tag() default "*";

    /**
     * 消费模式，包括集群模式、广播模式<br/>
     * 广播模式：同一个Group ID所标识的所有Consumer平均分摊消费消息<br/>
     * 广播模式：同一个Group ID所标识的所有Consumer都会各自消费某条消息一次<br/>
     * 请确保同一个Group ID下所有Consumer实例的订阅关系保持一致<br/>
     * 广播模式不支持顺序消息<br/>
     */
    MessageModel messageModel() default MessageModel.CLUSTERING;

    /**
     * 消费线程数量
     */
    String consumeThreadNums() default "";

    /**
     * 消息消费失败时的最大重试次数
     */
    String maxReconsumeTimes() default "";

    /**
     * 设置每条消息消费的最大超时时间,超过这个时间,这条消息将会被视为消费失败,等下次重新投递再次消费. 每个业务需要设置一个合理的值. 单位(分钟). 默认15分钟.
     */
    String consumeTimeout() default "";

    /**
     * BatchConsumer每次批量消费的最大消息数量, 默认值为1, 允许自定义范围为[1, 1024], 实际消费数量可能小于该值.
     */
    String consumeMessageBatchMaxSize() default "";

    /**
     * 设置本地批量消费聚合时间. 默认是0, 即消息从服务端取到之后立即开始消费. 该时间最大为ConsumeTimeout的一半. 最多450秒.
     */
    String batchConsumeMaxAwaitDurationInSeconds() default "";

    /**
     * 本地启动程序时是否注册消费者到RockerMQ服务器上
     */
    boolean localRegister() default false;

    /**
     * 指定了当前注解中，消费线程数量、最大重试次数等属性配置，是否用于初始化消费者客户端。
     * 仅当多个注解共用一个 consumerGroup 时生效。
     */
    boolean primary() default true;
}
