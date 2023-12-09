package site.btyhub.ons.config.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public enum MessageListenerTypeEnum {
    NORMAL("普通消息"),
    ORDER("顺序消息"),
    BATCH("批量消息"),
    ;

    private final String desc;
}
