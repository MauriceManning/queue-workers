package edu.berkeley.path.queue_workers.integration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 *  Contains the configuration information for JMS integration.
 */
@Configuration
@Profile("test")
public class JmsTestConfiguration {

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setUseCompression(true);

        //Dev ActiveMQ server on via-pn2.path.berkeley.edu
        activeMQConnectionFactory.setBrokerURL("tcp://169.229.249.10:61616");

        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(activeMQConnectionFactory);
        connectionFactory.setSessionCacheSize(10);
        connectionFactory.setCacheProducers(false);
        //connectionFactory.setCacheConsumers(false);

        return connectionFactory;
    }


    @Bean
    public JmsTemplate jmsStatusTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestinationName("Status");
        // ***  True = Topic, False = Queue   ***
        jmsTemplate.setPubSubDomain(true);
        return jmsTemplate;
    }


    @Bean
    public Queue requestsQueue() {
        return new ActiveMQQueue("PersistRequest");
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestination(requestsQueue());
        // ***  True = Topic, False = Queue   ***
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }




}
