package bexchange.eth.config;

import bexchange.eth.storage.scheduled.EthereumBlockListenerQueuesReceiver;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RabbitMQConfiguration {

    public static String exchangeName = "eth-explorer";
    public static String ethBlocksQueueName = "eth-block";
    public static String processBlockNumber = "eth-process-block";
    @Autowired
    public ConnectionFactory connectionFactory;
    @Autowired
    RabbitTemplate rabbitTemplate;
    private DirectExchange exchange = new DirectExchange(exchangeName, true, false);
    private Queue ethBlocksQueue;
    private Queue ethProcessBlockNumber;

    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin amqpAdmin = new RabbitAdmin(connectionFactory);

        amqpAdmin.declareExchange(exchange());

        amqpAdmin.declareQueue(ethBlocksQueue());
        amqpAdmin.declareQueue(ethProcessBlockNumberQueue());

        return amqpAdmin;
    }

    @Bean
    DirectExchange exchange() {
        return exchange;
    }


    @Bean
    Queue ethBlocksQueue() {
        if (ethBlocksQueue != null)
            return ethBlocksQueue;

        ethBlocksQueue = new Queue(ethBlocksQueueName, true, false, false, new HashMap<>());
        return ethBlocksQueue;
    }

    @Bean
    Queue ethProcessBlockNumberQueue() {
        if (ethProcessBlockNumber != null)
            return ethProcessBlockNumber;

        ethProcessBlockNumber = new Queue(processBlockNumber, true, false, false, new HashMap<>());
        return ethProcessBlockNumber;
    }

    @Bean
    Binding bindingEthBlocks() {
        return BindingBuilder.bind(ethBlocksQueue()).to(exchange()).withQueueName();
    }


    @Bean
    Binding bindingEthProcessBlock() {
        return BindingBuilder.bind(ethProcessBlockNumberQueue()).to(exchange()).withQueueName();
    }


    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(ethBlocksQueueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(EthereumBlockListenerQueuesReceiver receiver) {
        return new MessageListenerAdapter(receiver, "handleMessage");
    }


}
