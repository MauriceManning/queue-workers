package edu.berkeley.path.queue_workers.workers;

import core.oraDatabase;
import edu.berkeley.path.model_database_access.scenario.ScenarioWriter;
import edu.berkeley.path.model_objects.ModelObjectsFactory;
import edu.berkeley.path.model_objects.shared.RunRequest;
import edu.berkeley.path.queue_workers.integration.Publish;
import edu.berkeley.path.queue_workers.processing.XMLScenarioImporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.berkeley.path.model_objects.scenario.Scenario;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the listener class for the scenario channel. When a scenario is
 * received the worker will persist into the database
 */

@Component
public class PersistWorker {

    private Connection connection;
    private ScenarioWriter scenarioWriter;

    @Autowired
    private Publish publish;

    @Autowired
    private XMLScenarioImporter xmlImporter;


    public long persistScenario(HashMap message) {

        Long retval = new Long("0");

        final Logger logger = LogManager.getLogger(PersistWorker.class.getName());

        try {
            String scenarioXML = (String) message.get("Scenario");
            edu.berkeley.path.model_objects.util.Serializer serializer = new edu.berkeley.path.model_objects.util.Serializer();
            Scenario pScenario = serializer.xmlToObject(scenarioXML, Scenario.class, new ModelObjectsFactory() );

            //retrieve the request tag, currently an int but stored in a string
            String requestTag = (String) message.get("RequestId");
            logger.debug("requestId : " + requestTag);

            if (pScenario.getListOfNetworks().size() != 1 ) {
                logger.error(" Scenario must have exactly one network. Sceanrio id: " + pScenario.getId());
                return 0;
            }

            retval = xmlImporter.saveScenarioForNetwork(pScenario, pScenario.getListOfNetworks().get(0).getId());
            logger.info("ScenarioWorker handleMessage: insert scenario " + retval);

            HashMap map = new HashMap();
            map.put( "Id", retval);

            publish.publishStatus(requestTag, "PersistWorker", "Persisted",  "Scenario persisted into repository.", map);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retval;

    }

}
