package site.btyhub.ons;

import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class OnsMqTemplateProperties {

    private String nameServer;
    private String accessKey;
    private String secretKey;
    /**
     * Group name of producer.
     */
    private String group;
    /**
     * Millis of send message timeout.
     */
    @Builder.Default
    private int sendMessageTimeout = 3000;
}
