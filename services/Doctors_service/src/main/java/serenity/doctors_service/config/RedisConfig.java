package serenity.doctors_service.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import serenity.doctors_service.service.RedisSubscriber;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            MessageListenerAdapter chatListenerAdapter,
            MessageListenerAdapter approveContractListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("doctor-verifications"));
        container.addMessageListener(chatListenerAdapter, new PatternTopic("chat-messages"));
        container.addMessageListener(approveContractListenerAdapter, new PatternTopic("approve-contract"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "receiveMessage");
    }

    @Bean
    public MessageListenerAdapter chatListenerAdapter(RedisSubscriber chatSubscriber) {
        return new MessageListenerAdapter(chatSubscriber, "receiveChatMessage");
    }

    @Bean
    public MessageListenerAdapter approveContractListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "receiveApproveContract");
    }
}