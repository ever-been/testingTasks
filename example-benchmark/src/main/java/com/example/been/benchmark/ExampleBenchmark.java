package com.example.been.benchmark;

import cz.cuni.mff.d3s.been.benchmarkapi.Benchmark;
import cz.cuni.mff.d3s.been.benchmarkapi.BenchmarkException;
import cz.cuni.mff.d3s.been.benchmarkapi.ContextBuilder;
import cz.cuni.mff.d3s.been.core.task.TaskContextDescriptor;
import cz.cuni.mff.d3s.been.core.task.TaskContextState;
import cz.cuni.mff.d3s.been.util.PropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.been.benchmark.PropertyHelper.*;

/**
 * @author Martin Sixta
 */
public class ExampleBenchmark extends Benchmark {

	/**
	 * logging
	 */
	private static final Logger log = LoggerFactory.getLogger(ExampleBenchmark.class);

	private static final String STORAGE_CURRENT_RUN_KEY = "benchmark.run.current";
	private static final String STORAGE_CURRENT_RUN_DEFAULT_VALUE = "0";
	private static final String CLIENT_TEMPLATE_NAME = "client";
	private static final int BENCHMARK_RUNS_DEFAULT_VALUE = 10;

	@Override
	public TaskContextDescriptor generateTaskContext() throws BenchmarkException {

		PropertyReader propertyReader = createPropertyReader();

		// get current run from benchmark storage
		Integer currentRun = Integer.valueOf(storageGet(STORAGE_CURRENT_RUN_KEY, STORAGE_CURRENT_RUN_DEFAULT_VALUE));

		// get value of the task property
		int maxRuns = propertyReader.getInteger(BENCHMARK_RUNS_KEY, BENCHMARK_RUNS_DEFAULT_VALUE);


		log.debug("Running context generator for run {}", currentRun);

		if (currentRun >= maxRuns || maxRuns <= 0) {
			log.debug("No more context will be generated. Benchmark will end.");
			return null;
		} else {

			// how many clients?
			int numberOfClients = propertyReader.getInteger(CLIENT_COUNT_KEY, 2);
			int messagesPerClient = propertyReader.getInteger(CLIENT_MESSAGES_KEY, 1000);

			if (numberOfClients <= 0) {
				log.error("Number of clients must be bigger than zero");
				return null;
			}

			storageSet(STORAGE_CURRENT_RUN_KEY, (++currentRun).toString());

			return generateContext(currentRun, numberOfClients, messagesPerClient);

		}

	}

	private TaskContextDescriptor generateContext(Integer run, Integer numberOfClients, Integer messagesPerClient) throws BenchmarkException {

		// create context from a template
		ContextBuilder builder = ContextBuilder.createFromResource(ExampleBenchmark.class, "ContextTemplate.tcd.xml");


		// let the context know, how many clients there are
		builder.setProperty(CLIENT_COUNT_KEY, numberOfClients.toString());
		builder.setProperty(CLIENT_MESSAGES_KEY, messagesPerClient.toString());

		// add appropriate number of contexts
		for (int i = 0; i < numberOfClients; ++i) {
			final String clientName = String.format("Client%s", i);

			builder.addTask(clientName, CLIENT_TEMPLATE_NAME);

		}


		// create the context
		return builder.build();
	}

	@Override
	public void onResubmit() {

	}

	@Override
	public void onTaskContextFinished(String taskContextId, TaskContextState state) {
	}
}
