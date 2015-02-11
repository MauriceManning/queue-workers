package edu.berkeley.path.queue_workers;

import edu.berkeley.path.queue_workers.workers.PersistWorker;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *   http://java.dzone.com/articles/spring-integration-building
 */

public class WorkerApp {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("queue-workers-config.xml");
        context.start();


    }

}