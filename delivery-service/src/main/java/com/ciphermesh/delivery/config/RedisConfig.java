package com.ciphermesh.delivery.config;

import com.ciphermesh.delivery.routing.InstanceMessageChannel;
import com.ciphermesh.delivery.session.InstanceIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Subscribes this instance to its own Redis pub/sub channel so envelopes
 * forwarded from other instances (for a recipient connected here) are delivered
 * over the local WebSocket.
 */
@Configuration
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                InstanceMessageChannel instanceMessageChannel,
                                                                InstanceIdentity instanceIdentity) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(instanceMessageChannel,
                new ChannelTopic(InstanceMessageChannel.channelFor(instanceIdentity.id())));
        return container;
    }
}
