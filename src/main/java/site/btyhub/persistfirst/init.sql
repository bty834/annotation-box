

create table if not exists local_persist
(
    id               bigint auto_increment primary key,
    bean_name        varchar(63) not null,
    method_signature text,
    args             text,
    state            tinyint(1)  not null comment '0待重试 1重试成功',
    retry_times      int(11)     not null comment '重试次数',
    next_retry_time  timestamp   not null comment '下次重试时间',
    create_time      timestamp   not null,
    update_time      timestamp   not null,
    key idx_next_retry_time_state (state, next_retry_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
