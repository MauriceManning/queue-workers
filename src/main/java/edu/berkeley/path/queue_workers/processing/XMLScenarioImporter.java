package edu.berkeley.path.queue_workers.processing;


import core.oraDatabase;
import edu.berkeley.path.model_database_access.MODatabaseException;
import edu.berkeley.path.model_database_access.network.NetworkWriter;
import edu.berkeley.path.model_database_access.scenario.ScenarioWriter;
import edu.berkeley.path.model_objects.MOException;
import edu.berkeley.path.model_objects.ModelObjectsFactory;
import edu.berkeley.path.model_objects.network.Network;
import edu.berkeley.path.model_objects.scenario.Scenario;
import edu.berkeley.path.model_objects.scenario.SplitRatioProfile;
import edu.berkeley.path.model_objects.scenario.SplitRatioSet;
import edu.berkeley.path.model_objects.scenario.Splitratio;
import edu.berkeley.path.model_objects.util.Serializer;
import edu.berkeley.path.model_orm.model.ITurnMovement;
import edu.berkeley.path.model_orm.model.impl.TurnMovement;
import edu.berkeley.path.model_orm.producer.FundamentalDiagramController;
import edu.berkeley.path.model_orm.producer.ProjectController;
import edu.berkeley.path.model_orm.model.impl.Project;
import edu.berkeley.path.model_orm.model.IProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by tmk on 3/13/15.
 */

@Component
public class XMLScenarioImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(XMLScenarioImporter.class);

    @Autowired
    private ProjectController projectController;

    @Autowired
    private FundamentalDiagramController fundamentalDiagramProducer;

    /**
     * Import a given scenario String, defining a new project with name pName and description pDescription.  Will
     * define a name or description if the values passed in are empty or null.
     *
     * @param scenarioXML     The whole XML of the scenario (very large)
     * @param pName           Desired name of new project (under 64 bytes)
     * @param pDescription    Desired description of new project
     *
     * @return ID of new project, or null on failure.
     */
    public Long createNewProjectFromScenario(String scenarioXML, String pName, String pDescription) {

        Connection connection = null;
        Scenario scenario = null;
        Long scenId = null;
        long projectId = 0;

        // connect to DB
        connection = oraDatabase.doConnect();
        if (connection == null) {
            LOGGER.error("Can't connect to database - returning.");
            return null;
        }

        try {
            scenario = Serializer.xmlToObject(scenarioXML, Scenario.class, new ModelObjectsFactory());
        } catch (MOException e) {
            LOGGER.error("Can't convert scenario data to object: {}", e.getMessage());
            return null;
        }

        //  Save the Scenario here

        // TODO: NOTE NOTE NOTE NOTE NOTE
        // TODO: This relies on ScenarioWriter from ModelDatabaseAccess, but tweaks the scenario before writing it
        // TODO: NOTE NOTE NOTE NOTE NOTE

        // Construct a whole new project for this XML scenario, treating the network as a CTM network and adding all
        // the scenario's sets. Need to construct TurnMovements to add Splits and SignalPlans

        IProject project = new Project();
        if ((null != pName) && (!pName.equals(""))) {
            project.setName(pName);
        } else {
            project.setName("Imported Project: " + scenario.getName());
        }
        if ((null != pDescription) && (! pDescription.equals(""))) {
            project.setDescription(pDescription);
        } else {
            project.setDescription("Project constructed by automated import of an XML Scenario: " + scenario.getDescription());
        }
        project.setInUse(1); // Make it show up in the ProjectManager...

        projectId = projectController.addProject(project);
        LOGGER.debug("Created project for {}", scenario.getName());

        if (! setProjectId(scenario, projectId)) {
            LOGGER.error("Failed setting project ID for the scenario sets");
            return null;
        }

        // Handle the scenario network carefully -- we need it to be entered before we can construct turn movements
        // This also means the project will have a single network of type CTM.
        Network network = scenario.getListOfNetworks().get(0);
        network.setNetworkType(3, "CTM", "");
        network.setProjectId(projectId);
        Long networkID = 0L;

        NetworkWriter networkWriter = new NetworkWriter(connection);

        try {
            networkID = networkWriter.insert(network,0L); // 0L keeps ids as assigned in XML
        } catch (MODatabaseException e) {
            e.printStackTrace();
            // Cleanup the stale project entry
            projectController.deleteProject(project);
            return null;
        }
        LOGGER.debug("Wrote network for {}", scenario.getName());

        scenario.getSplitRatioSet().setNetworkId(networkID);

        // Can construct TurnMovements now that project and networkID are defined
        generateTurnMovements(scenario.getSplitRatioSet(), networkID, project);
        LOGGER.debug("Wrote TurnMovements for {}", scenario.getName());

        // With TurnMovements added, we should be able to insert the scenario (handled its network above)
        scenId = saveScenarioForNetwork(scenario, networkID);

        if (null != scenId) {
            LOGGER.info("SAVED scenario! New Project ID is: {}; New Scenario ID is: {}", projectId, scenId);
        } else {
            projectController.deleteProject(project);
            //TODO: clean up the network and turnmovements!
        }

        // Clean up DB connection
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("Error closing database connection: " + e.getMessage());
        }

        return projectId;
    }

    /**
     * Import a given scenario String, defining a new project with name pName and description pDescription.  Will
     * define a name or description if the values passed in are empty or null.
     *
     * @param scenario        A complete model-objects scenario
     * @param networkID       ID of the network in the database that the scenario refers to
     *
     * @return ID of the new scenario in the DB, or null on failure
     */
    public Long saveScenarioForNetwork(Scenario scenario, long networkID) {

        Long scenId = null;
        // connect to DB
        Connection connection  = oraDatabase.doConnect();
        if (connection == null) {
            LOGGER.error("Can't connect to database - returning.");
            return null;
        }
        scenario.setNetworkSet(null); // Don't want the Network to be entered from the scenario -- assume that's been done

        try{
            ScenarioWriter scenarioWriter = new ScenarioWriter(connection);

            scenId = scenarioWriter.insert(scenario);

            // Insert the NetworkSets row now because the scenario did not have the network, and we don't get the
            // scenario ID until it's inserted (above). NetworkSets is the only DB mapping of network to scenario
            scenarioWriter.insertNetworkSetRow(networkID,scenId);

        } catch (MODatabaseException modbe) {
            LOGGER.error("Failed to save scenario {}: {}", scenario.getName(), modbe.getMessage());
            return null;
        }
        return scenId;
    }

    private boolean setProjectId(Scenario scenario, long projectId) {

        scenario.setProjectId(projectId);

        if (null != scenario.getActuatorSet()) {
            scenario.getActuatorSet().setProjectId(projectId);
        }
        if (null != scenario.getControllerSet()) {
            scenario.getControllerSet().setProjectId(projectId);
        }
        if (null != scenario.getDemandSet()) {
            scenario.getDemandSet().setProjectId(projectId);
        }
        if (null != scenario.getFundamentalDiagramSet()) {
            scenario.getFundamentalDiagramSet().setProjectId(projectId);
        }
        if (null != scenario.getInitialDensitySet()) {
            scenario.getInitialDensitySet().setProjectId(projectId);
        }
        if (null != scenario.getNetworkSet()) {
            scenario.getNetworkSet().setProjectId(projectId);
        }
        if (null != scenario.getRouteSet()) {
            scenario.getRouteSet().setProjectId(projectId);
        }
        if (null != scenario.getSensorSet()) {
            scenario.getSensorSet().setProjectId(projectId);
        }
        if (null != scenario.getSplitRatioSet()) {
            scenario.getSplitRatioSet().setProjectId(projectId);
        }

        return true ;
    }

    /**
     * Create TurnMovements for every inlink-outlink pair as defined by the SplitRatioSet
     * @param splitRatioSet scenario's SplitRatioSet
     * @param networkID     ID assigned to scenario Network (must be written to DB first)
     * @param project       Project object created for the imported scenario
     */
    private  void generateTurnMovements(SplitRatioSet splitRatioSet, Long networkID, IProject project) {

        int counter = 0;

        if (null == splitRatioSet) return;

        for (SplitRatioProfile srProfile : splitRatioSet.getListOfSplitRatioProfiles()) {
            for (Splitratio splitratio : srProfile.getListOfSplitratios()) {

                ITurnMovement turnMovement = new TurnMovement();
                turnMovement.setNetworkId(networkID);
                turnMovement.setProject(project);
                turnMovement.setInLinkId(splitratio.getLinkIn());
                turnMovement.setOutLinkId(splitratio.getLinkOut());
                turnMovement.setAllowed(true);
                // tmList.add(turnMovement);
                fundamentalDiagramProducer.addTurnMovement(turnMovement); //addTurnMovementCO(turnMovement);
                counter++;
            }
        }
        LOGGER.info("Inserted {} Turn Movements from SplitRatioSet", counter);
    }

}
