package edu.berkeley.path.queue_workers.integration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 * Contains the configuration information for JMS integration.
 */

@Configuration
@Profile("dev")
public class JmsDevConfiguration {

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory();

        // local install of ActiveMQ for development testing
        activeMQConnectionFactory.setBrokerURL("tcp://localhost:61616");
        //activeMQConnectionFactory.setUseCompression(true);

        //embedded ActiveMQ broker when one is not installed locally.
        //activeMQConnectionFactory.setBrokerURL("vm://localhost");

        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(activeMQConnectionFactory);
        connectionFactory.setSessionCacheSize(10);
        connectionFactory.setCacheProducers(false);
        //connectionFactory.setCacheConsumers(false);

        return connectionFactory;
    }

//    @Bean
//    public Queue requestsQueue() {
//        return new ActiveMQQueue("scenarioChannel");
//    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        //jmsTemplate.setDefaultDestination(requestsQueue());
        //jmsTemplate.setDefaultDestinationName("scenarioPublish");
        // ***  True = Topic, False = Queue   ***
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }


}
