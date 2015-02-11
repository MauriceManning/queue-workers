package edu.berkeley.path.queue_workers.workers;

import core.oraDatabase;
import edu.berkeley.path.model_database_access.scenario.ScenarioWriter;
import edu.berkeley.path.model_objects.ModelObjectsFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import edu.berkeley.path.model_objects.scenario.Scenario;

import java.sql.Connection;

/**
 * This is the listener class for the scenario channel. When a scenario is
 * received the worker will persist into the database
 */

@Component
public class PersistWorker {

    private Connection connection;
    private ScenarioWriter scenarioWriter;


    public long persistScenario(String xmlScenario) {

        Long retval = new Long("0");

        final Logger logger = LogManager.getLogger(PersistWorker.class.getName());



        try {
            edu.berkeley.path.model_objects.util.Serializer serializer = new edu.berkeley.path.model_objects.util.Serializer();
            Scenario pScenario = serializer.xmlToObject(xmlScenario, Scenario.class, new ModelObjectsFactory() );

            connection = oraDatabase.doConnect();
            scenarioWriter = new ScenarioWriter(connection);
            retval = scenarioWriter.insert(pScenario);
            logger.info("ScenarioWorker handleMessage: insert scenario " + retval);

        } catch (Exception e) {
            e.printStackTrace();
            // assert fails if exception is thrown
        }

        return retval;

    }



}
