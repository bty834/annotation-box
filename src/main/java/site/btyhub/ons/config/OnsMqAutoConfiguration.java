package site.btyhub.ons.config;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class OnsMqAutoConfiguration {

    @Bean
    public MessageListenerRegister messageListenerRegister(ConfigurableApplicationContext context, StandardEnvironment environment){
        return new MessageListenerRegister(context, environment);
    }

}
