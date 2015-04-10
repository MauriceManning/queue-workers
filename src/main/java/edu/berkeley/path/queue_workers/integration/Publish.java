package edu.berkeley.path.queue_workers.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to publish information to JMS.
 */

@Component
public class Publish {


    @Autowired
    private JmsTemplate jmsStatusTemplate;



    public void publishStatus(long taskId, String publisher, String type, String description, HashMap details) {

        final Logger logger = LogManager.getLogger(Publish.class.getName());

        final long TaskIdFinal = taskId;
        final String TypeFinal = type;

        Map map = new HashMap();
        map.put( "RequestId", taskId);
        map.put( "Publisher", publisher);
        map.put( "Type", type);
        map.put( "Description", description);
        map.put( "Details", details);

        try {

            jmsStatusTemplate.convertAndSend("Status", map, new MessagePostProcessor() {
                public Message postProcessMessage(Message message) throws JMSException {
                    message.setLongProperty("Task", TaskIdFinal  );
                    message.setStringProperty("Type", TypeFinal  );
                    return message;
                }
            });

        } catch (Exception e) {
            logger.trace("publishStatus RequestId " + TaskIdFinal);
            logger.trace("publishStatus Type " + TypeFinal);

            e.printStackTrace();
        }

    }

}
