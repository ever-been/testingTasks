package com.example.been.benchmark.common;

/**
 * @author Martin Sixta
 */
public class PropertyHelper {
	public static String CLIENT_COUNT_KEY = "clients.count";
	public static String CLIENT_MESSAGES_KEY = "clients.messages";

	public static String BENCHMARK_RUNS_KEY = "benchmark.runs";

	public static int DEFAULT_TIMEOUT = 10000;


	public static int getNumberOfClients() {
		return Integer.valueOf(System.getenv(CLIENT_COUNT_KEY));
	}

	public static int getNumberOfMessages() {
		return Integer.valueOf(System.getenv(CLIENT_MESSAGES_KEY));
	}

	public static int getTotalNumberOfConnections() {
		return getNumberOfClients() * getNumberOfMessages();
	}


	public static String CONTEXT_RENDEZVOUS = "rendezvous";

	public static String CONTEXT_CHECKPOINT_ADDRESS = "address";

	public static String CONTEXT_CHECKPOINT_GO = "go";

	public static String RESULT_GROUP = "example-benchmark";

	public static String LOG_LEVEL_KEY = "task.log.level";

	public static String CURRENT_RUN_KEY = "run";


}
