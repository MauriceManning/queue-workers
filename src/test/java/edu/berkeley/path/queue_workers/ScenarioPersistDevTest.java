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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:queue-workers-config.xml"})
public class ScenarioPersistDevTest {

    private Logger logger = null;
    private static Connection connection;
    private static Scenario scenario;
    private static ScenarioWriter scenarioWriter;
    private static ScenarioReader scenarioReader;

    //used to mini scenario test
    public static final long PROJECT_ID = 99999L;
    public static final String NAME = "UNIT TEST SCENARIO";
    public static final String DESCRIPTION = "UNIT TEST SCENARIO";
    public static final boolean LOCKED = false;
    public static final long SET_ID = 1L;


    @Autowired
    private JmsTemplate jmsTemplate;
    public void setJmsPublishTemplate(JmsTemplate jt) { this.jmsTemplate = jt; }

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
    //@Ignore
    public void testMiniScenarioPublish() {
        try {

            scenario = new Scenario();
            scenario.setProjectId(PROJECT_ID);
            scenario.setName(NAME);
            scenario.setDescription(DESCRIPTION);
            scenario.setActuatorSetId(SET_ID);
            scenario.setDemandSetId(SET_ID);
            scenario.setFdSetId(SET_ID);
            scenario.setSensorSetId(SET_ID);
            scenario.setDemandSetId(SET_ID);
            scenario.setSplitRatioSetId(SET_ID);
            scenario.setLockedForEdit(LOCKED);
            scenario.setLockedForHistory(LOCKED);

            jmsTemplate.setReceiveTimeout(3000);


            try {
                edu.berkeley.path.model_objects.util.Serializer serializer = new edu.berkeley.path.model_objects.util.Serializer();
                String xmlScenario = serializer.objectToXml(scenario);

                jmsTemplate.convertAndSend("scenarioPublish", xmlScenario);

                // now catch the id of the inserted scenario
                Message<?>  message =  testChannel.receive(2000);
                assertNotNull("Expected a scenario id in the response", message);

                //retrieve the scenario id from the payload then delete to clean up
                Long scenarioId = (Long) message.getPayload();
                assertNotEquals(scenarioId.longValue(), 0L);
                logger.info("ScenarioPublishTest clean up scenario. Id: " + scenarioId);
                scenario = scenarioReader.readRow(scenarioId);
                scenarioWriter.deleteRow(scenario);


            } catch (Exception e) {
                e.printStackTrace();
                // assert fails if exception is thrown
            }

        } catch (Exception ex) {
            logger.info("ScenarioPublishTest publish  Exception ex:" + ex.getMessage());
            ex.printStackTrace();

            // assert fails if exception is thrown
        }
    }


    public void onMessage(javax.jms.Message message) { logger.info("onMessage:  update received" ); }

}
