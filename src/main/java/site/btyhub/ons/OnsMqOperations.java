package site.btyhub.ons;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.SendResult;

/**
 * 如果需要发送定时消息,请使用 Message,参考 <br/>
 * Message msg = new Message( <br/>
 * &#9  // 您在消息队列RocketMQ版控制台创建的Topic。<br/>
 * &#9  "Topic",<br/>
 * &#9  // Message Tag,可理解为Gmail中的标签，对消息进行再归类，方便Consumer指定过滤条件在消息队列RocketMQ版服务器过滤。<br/>
 * &#9  "tag",<br/>
 * &#9  // Message Body可以是任何二进制形式的数据，消息队列RocketMQ版不做任何干预，需要Producer与Consumer协商好一致的序列化和反序列化方式。<br/>
 * &#9  "Hello MQ".getBytes());<br/>
 *
 * // 设置消息需要被投递的时间。<br/>
 * msg.setStartDeliverTime(timeStamp);<br/>
 *
 * @author huangguoping@baijiahulian.com
 * @date 2021/12/7
 */
public interface OnsMqOperations {

    /**
     * @param topic 主题名称.
     * @param tags  设置消息标签.
     * @param body  String类型消息体.
     * @return .
     */
    SendResult send(String topic, String tags, String body);

    /**
     * 发送有序消息
     *
     * @param topic       主题名称.
     * @param tags        设置消息标签.
     * @param body        String类型消息体.
     * @param shardingKey 有序消息中同一shardingKey队列相同.
     * @return .
     */
    SendResult sendOrderMsg(String topic, String tags, String body, String shardingKey);

    /**
     * 单向发送
     *
     * @param topic 主题名称.
     * @param tags  设置消息标签.
     * @param body  String类型消息体.
     * @return .
     */
    boolean sendOneWayMes(String topic, String tags, String body);

    /**
     * @param message 消息体.
     * @return .
     */
    SendResult sendMsg(Message message);

    /**
     * 发送有序消息
     *
     * @param message     消息体.
     * @param shardingKey 分片键.
     * @return .
     */
    SendResult sendOrderMsg(Message message, String shardingKey);

    /**
     * 单向发送
     *
     * @param message 消息体.
     * @return .
     */
    boolean sendOneWayMes(Message message);
}
