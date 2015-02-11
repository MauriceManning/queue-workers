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
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;

import static junit.framework.Assert.fail;

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

    @Before
    public void setUp() throws Exception {

        this.logger = LogManager.getLogger(FullScenarioPersistTest.class.getName());

        connection = oraDatabase.doConnect();
        scenarioWriter = new ScenarioWriter(connection);
        scenarioReader = new ScenarioReader(connection);

//        // create a simple scenario
//        scenario = new Scenario();
//        scenario.setName("testScenario");

    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    //@Ignore
    public void testScenarioPublish() {
        try {

            jmsTemplate.setReceiveTimeout(3000);


            Scenario bigScenario;

            // load a scenario to publish
            bigScenario = scenarioReader.read(SCENARIO_ID);

            try {
                edu.berkeley.path.model_objects.util.Serializer serializer = new edu.berkeley.path.model_objects.util.Serializer();
                String xmlScenario = serializer.objectToXml(bigScenario);

                //for (int i=0; i < 3; i++) {
                jmsTemplate.convertAndSend(xmlScenario);


            } catch (Exception e) {
                e.printStackTrace();
                // assert fails if exception is thrown
            }

        } catch (Exception ex) {
            logger.info("ScenarioPublishTest publish  Exception ex:" + ex.getMessage());
            logger.info("ScenarioPublishTest publish  Exception2 ex:" + ex);

            // assert fails if exception is thrown
        }

    }



}
