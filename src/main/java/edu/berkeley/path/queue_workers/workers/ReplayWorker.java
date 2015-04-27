package edu.berkeley.path.queue_workers.workers;


import edu.berkeley.path.model_orm.model.ILinkDataTotal;
import edu.berkeley.path.model_orm.model.impl.LinkDataTotal;
import edu.berkeley.path.queue_workers.integration.Publish;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;

/**
 *
 */
@Component
public class ReplayWorker {

    private Connection connection;


    @Autowired
    private Publish publish;

    @Autowired
    SessionFactory sessionFactory;

    public long readResults(HashMap message) {

        Long retval = new Long("0");

        final Logger logger = LogManager.getLogger(PersistWorker.class.getName());

        try {
            String resultsId = (String) message.get("ResultsId");

            //retrieve the request tag, currently an int but stored in a string
            long taskId =  ( (Long) message.get("TaskId")).longValue();
            logger.debug("taskId : " + taskId);

            //http://docs.jboss.org/hibernate/core/3.3/reference/en/html/batch.html#batch-statelesssession

            StatelessSession session = sessionFactory.openStatelessSession();
            Transaction tx = session.beginTransaction();

            Query query = session.createQuery("from LinkDataTotal where appRunId = :appRunId");
            query.setParameter("appRunId", resultsId );
            ScrollableResults scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);

            boolean firstStep = true;
            Date timeStep = null;
            while ( scrollableResults.next() ) {
                LinkDataTotal linkDataTotal = (LinkDataTotal) scrollableResults.get(0);
                if (firstStep) {
                    timeStep = linkDataTotal.getTimeStep();
                    firstStep = false;
                }





            }

            tx.commit();
            session.close();


            publish.publishStatus(taskId, "ReplayWorker", "Queued",  "Results replayed from the repository.", null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retval;

    }



}
