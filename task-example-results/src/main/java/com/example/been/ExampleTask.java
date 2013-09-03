package com.example.been;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.persistence.Query;
import cz.cuni.mff.d3s.been.persistence.ResultQueryBuilder;
import cz.cuni.mff.d3s.been.taskapi.Persister;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.example.been.ExampleResult.GROUP_ID;

/**
 * Example BEEN task displaying basic concepts and API behind results.
 *
 * @author Martin Sixta
 */
public class ExampleTask extends Task {

	/**
	 * Tasks use standard logging interface.
	 */
	private static final Logger log = LoggerFactory.getLogger(ExampleTask.class);

	@Override
	public void run(String[] strings) throws TaskException, MessagingException, DAOException {
		log.info("Running the ExampleTask");

		// To properly initialize a result the `createResult` factory should be used
		// taskId, contextId, benchmarkId (if running as part of a benchmark) will be filled in
		ExampleResult result1 = results.createResult(ExampleResult.class);
		ExampleResult result2 = results.createResult(ExampleResult.class);


		result1.setData(47);
		result1.setName("Name47");
		result2.setData(42);
		result2.setName("Name42");

		// persister implements AutoClosable
		try (Persister persister = results.createResultPersister(GROUP_ID)) {
			log.info("Persisting a result");
			persister.persist(result1);
			persister.persist(result2);
		}

		try {
			// Wait a while for the result to be persisted - persisting is asynchronous
			log.info("Waiting for a while");
			Thread.sleep(3000); // 3 seconds should be enough for everybody

		} catch (InterruptedException e) {
			throw new TaskException("Tasks interrupted", e);
		}


		log.info("Querying all results");

		// Queries are build with the ResultQueryBuilder which has fluent API

		// notice the on() and with() method usage
		Query queryAll = new ResultQueryBuilder().on(GROUP_ID).with("taskId", getId()).fetch();

		Collection<ExampleResult> taskResults = results.query(queryAll, ExampleResult.class);

		for (ExampleResult result: taskResults) {
			log.info("Result fetched {}", result.getData());
		}

		log.info("Querying results with data == 47");
		// now lets only query results which data == 47
		Query queryWithData = new ResultQueryBuilder().on(GROUP_ID).with("taskId", getId()).with("data", 47).fetch();

		taskResults = results.query(queryWithData, ExampleResult.class);

		for (ExampleResult result: taskResults) {
			log.info("Result fetched {} (should be 47)", result.getData());
		}

		log.info("Querying results with name == name47, but only retrieving data field");
		// example how to query only for certain fields
		Query queryOnlyData = new ResultQueryBuilder().on(GROUP_ID).with("taskId", getId()).with("name", "Name42").retrieving("data").fetch();


		taskResults = results.query(queryOnlyData, ExampleResult.class);

		for (ExampleResult result: taskResults) {
			log.info("Result fetched with data field only: data {}, name {} (should be null))", result.getData(), result.getName());
		}


		log.info("Task exiting");

	}
}
