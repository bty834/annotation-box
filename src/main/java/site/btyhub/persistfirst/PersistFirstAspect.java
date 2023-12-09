package site.btyhub.persistfirst;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import site.btyhub.util.SpringUtil;

@Aspect
@Component
@Slf4j
@Data
@SuppressWarnings("all")
@Order(Ordered.LOWEST_PRECEDENCE - 50)
public class PersistFirstAspect implements PersistFirstCompensator{

    @Value("${persist.first.table.name:local_persist}")
    private String tableName ;

    @Value("${persist.first.compensate.interval.minutes:3}")
    private int compensateIntervalMinutes;

    @Value("${persist.first.max.retry.times:3}")
    private int maxRetryTimes;

    private Level logLevelOnFail = Level.ERROR;

    @Data
    @ToString
    static class PersistVO{
        private Long id;
        private String beanName;
        private String methodSignature;
        private String args;
        // 0-待重试 1-重试成功
        private Integer state;
        private Integer retryTimes;
        private LocalDateTime nextRetryTime;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
    private static final String SQL_INSERT = "insert into %s " +
            "(bean_name, method_signature, args, state, retry_times, next_retry_time, create_time, update_time)" +
            " values " +
            "(:bean_name, :method_signature, :args, :state, :retry_times, :next_retry_time, :create_time, :update_time)";

    private static final String SQL_UPDATE = "update %s " +
            "set " +
            " retry_times = :retryTimes," +
            " next_retry_time = :nextRetryTime"+
            " state = :state," +
            " update_time = :updateTime " +
            "where " +
            " id = :id";

    private static final String SQL_COMPENSATE = "select " +
            "id, bean_name, method_signature, args, state, retry_times, next_retry_time, create_time, update_time" +
            "from %s " +
            "where next_retry_time > now() and status = 0 and retry_times <= %s";

    private static final String SQL_COMPENSATE_ID = "select " +
            "id, bean_name, method_signature, args, state, retry_times, next_retry_time, create_time, update_time" +
            "from %s " +
            "where id = %s";

    private TransactionTemplate transactionTemplate;

    private NamedParameterJdbcTemplate jdbcTemplate;
    private KeyHolder keyHolder = new GeneratedKeyHolder();


    private ThreadLocal<Boolean> callFromCompensator = new ThreadLocal<>();

    @Around("@annotation(persistFirst)")
    public void around(ProceedingJoinPoint joinPoint, PersistFirst persistFirst) throws Throwable {


        Boolean b = callFromCompensator.get();
        // 补偿直接跳过这个aspect
        if(Objects.nonNull(b) && b){
            joinPoint.proceed();
            return;
        }

        String dataSourceBeanName = persistFirst.dataSourceBeanName();

        MethodSignature signature = (MethodSignature)joinPoint.getSignature();


        PersistVO vo = new PersistVO();
        vo.setBeanName(SpringUtil.getBeanNameByObject(joinPoint.getThis()));
        vo.setMethodSignature(JSON.toJSONString(signature));
        vo.setArgs(JSON.toJSONString(joinPoint.getArgs()));
        vo.setState(0);
        vo.setNextRetryTime(LocalDateTime.now().plusMinutes(compensateIntervalMinutes));
        vo.setUpdateTime(LocalDateTime.now());
        vo.setCreateTime(LocalDateTime.now());


        transactionTemplate.executeWithoutResult(transactionStatus -> {
            String sql  = String.format(SQL_INSERT,tableName);
            SqlParameterSource ps = new BeanPropertySqlParameterSource(vo);
            jdbcTemplate.update(sql, ps, keyHolder);
            Number id = keyHolder.getKey();
            if (id != null) {
                vo.setId(id.longValue());
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 添加监听器，在事务提交后触发后续任务
            TransactionSynchronization transactionSynchronization = new TransactionSynchronizationAdapter(){
                @Override
                public void afterCommit() {
                    try {
                        joinPoint.proceed();
                        vo.setState(1);
                        vo.setUpdateTime(LocalDateTime.now());
                        transactionTemplate.executeWithoutResult(transactionStatus -> {
                            String sql  = String.format(SQL_UPDATE,tableName);
                            SqlParameterSource ps = new BeanPropertySqlParameterSource(vo);
                            jdbcTemplate.update(sql, ps);

                        });
                    } catch (Throwable e) {
                        log("执行失败",e);
                    }
                }
            };
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
        } else {
            // 没有可以事务，直接触发后续任务
            try {
                joinPoint.proceed();
                vo.setState(1);
                vo.setUpdateTime(LocalDateTime.now());
                transactionTemplate.executeWithoutResult(transactionStatus -> {
                    try {
                        String sql  = String.format(SQL_UPDATE,tableName);
                        SqlParameterSource ps = new BeanPropertySqlParameterSource(vo);
                        jdbcTemplate.update(sql, ps, keyHolder);
                        Number id = keyHolder.getKey();
                        if (id != null) {
                            vo.setId(id.longValue());
                        }
                    } catch (Exception e) {
                        log("更新状态失败",e);
                    }
                });
            } catch (Exception e) {
                log("事务执行失败",e);
            }
        }

    }

    private void log(String msg,Throwable e){
        if(logLevelOnFail.equals(Level.INFO)){
            log.info(msg,e);
        } else if (logLevelOnFail.equals(Level.ERROR)){
            log.error(msg,e);
        } else if (logLevelOnFail.equals(Level.WARN)){
            log.warn(msg,e);
        } else if( logLevelOnFail.equals(Level.DEBUG)){
            log.debug(msg,e);
        } else if (logLevelOnFail.equals(Level.TRACE)){
            log.trace(msg,e);
        } else {
            log.info(msg,e);
        }
    }

    @Override
    public void compensate() {
        String sql = String.format(SQL_COMPENSATE,tableName,maxRetryTimes);
        doCompensate(sql);
    }


    @Override
    public void compensate(Integer id) {
        String sql = String.format(SQL_COMPENSATE_ID,tableName,id);
        doCompensate(sql);
    }

    private void doCompensate(String sql) {
        RowMapper<PersistVO> rowMapper = new BeanPropertyRowMapper<>(PersistVO.class);

        List<PersistVO> query = this.jdbcTemplate.query(sql, rowMapper);

        for (PersistVO persistVO : query) {
            try {
                callFromCompensator.set(Boolean.TRUE);
                invokeMethod(persistVO);
                persistVO.setState(1);
                persistVO.setUpdateTime(LocalDateTime.now());
            } catch (Exception e) {
                log("补偿失败",e);
                persistVO.setRetryTimes(persistVO.getRetryTimes()+1);
                persistVO.setNextRetryTime(LocalDateTime.now().plusMinutes(compensateIntervalMinutes));
                persistVO.setUpdateTime(LocalDateTime.now());
            } finally {
                callFromCompensator.remove();
            }
            String updateSql  = String.format(SQL_UPDATE,tableName);
            try {
                SqlParameterSource ps = new BeanPropertySqlParameterSource(persistVO);
                jdbcTemplate.update(sql, ps);
            } catch (Exception e) {
                log("更新状态失败",e);
            }

        }
    }

    private void invokeMethod(PersistVO persistVO) throws Exception {
        String beanName = persistVO.getBeanName();
        Object bean = SpringUtil.getBean(beanName);

        MethodSignature methodSignature = JSON.parseObject(persistVO.getMethodSignature(), MethodSignature.class);
        Object[] args = JSON.parseObject(persistVO.getArgs(), Object[].class);
        Method method = methodSignature.getMethod();
        method.setAccessible(true);
        method.invoke(bean,args);
    }
}
