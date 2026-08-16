package com.uniondesk.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 订阅：ud:rt:events 频道 → RealtimeEventPublisher.onMessage（多实例广播投递）。
 */
@Configuration
public class RealtimeRedisConfig {

    @Bean
    public RedisMessageListenerContainer realtimeRedisListenerContainer(
            RedisConnectionFactory connectionFactory,
            RealtimeEventPublisher publisher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(publisher, new ChannelTopic(RealtimeConstants.REDIS_CHANNEL));
        return container;
    }
}
