package edu.berkeley.path.queue_workers;

import core.oraDatabase;
import edu.berkeley.path.model_database_access.scenario.ScenarioReader;
import edu.berkeley.path.model_database_access.scenario.ScenarioWriter;
import edu.berkeley.path.model_objects.scenario.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.JMSException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:queue-workers-config.xml"})
public class FullScenarioPersistTest {

    private Logger logger = null;
    private static Connection connection;
    private static Scenario scenario;
    private static ScenarioWriter scenarioWriter;
    private static ScenarioReader scenarioReader;
    public static final int SCENARIO_ID = 20342;


    @Autowired
    private JmsTemplate jmsTemplate;
    public void setJmsTemplate(JmsTemplate jt) { this.jmsTemplate = jt; }

    @Autowired
    QueueChannel testChannel;

    @Before
    public void setUp() throws Exception {

        this.logger = LogManager.getLogger(FullScenarioPersistTest.class.getName());

        connection = oraDatabase.doConnect();
        scenarioWriter = new ScenarioWriter(connection);
        scenarioReader = new ScenarioReader(connection);

    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    @Ignore
    public void testScenarioPublish() {
        try {

            jmsTemplate.setReceiveTimeout(3000);


            Scenario bigScenario;

            // load a scenario to publish
            bigScenario = scenarioReader.read(SCENARIO_ID);

            try {
                edu.berkeley.path.model_objects.util.Serializer serializer = new edu.berkeley.path.model_objects.util.Serializer();
                String xmlScenario = serializer.objectToXml(bigScenario);

                final String RequestIdFinal = "r001";
                final String TypeFinal = "persist";

                Map map = new HashMap();
                map.put( "RequestId", RequestIdFinal);
                map.put( "Scenario", xmlScenario);

                jmsTemplate.convertAndSend("PersistRequest", map, new MessagePostProcessor() {
                    public javax.jms.Message postProcessMessage(javax.jms.Message message) throws JMSException {
                        message.setStringProperty("RequestId", RequestIdFinal  );
                        message.setStringProperty("Type", TypeFinal  );
                        return message;
                    }
                });

                // now catch the id of the inserted scenario
                Message<?> message =  testChannel.receive(5000);
                assertNotNull("Expected a scenario id in the response", message);

                //retrieve the scenario id from the payload then delete to clean up
                Long scenarioId = (Long) message.getPayload();
                assertNotEquals(scenarioId.longValue(), 0L);
                logger.info("ScenarioPublishTest clean up scenario. Id: " + scenarioId);
                scenario = scenarioReader.readRow(scenarioId);
                scenarioWriter.deleteRow(scenario);


            } catch (Exception e) {
                e.printStackTrace();

            }

        } catch (Exception ex) {
            logger.info("ScenarioPublishTest publish  Exception ex:" + ex.getMessage());
            logger.info("ScenarioPublishTest publish  Exception2 ex:" + ex);

        }

    }

}
