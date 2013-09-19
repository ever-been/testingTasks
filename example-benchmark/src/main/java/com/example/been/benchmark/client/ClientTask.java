package com.example.been.benchmark.client;

import com.example.been.benchmark.common.ExampleResult;
import com.example.been.benchmark.common.PropertyHelper;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import cz.cuni.mff.d3s.been.util.PropertyReader;
import org.jeromq.ZMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

import static com.example.been.benchmark.common.PropertyHelper.*;

/**
 * @author Martin Sixta
 */
public class ClientTask extends Task {

	/**
	 * logging
	 */
	private static final Logger log = LoggerFactory.getLogger(ClientTask.class);


	@Override
	public void run(String[] args) throws TaskException, MessagingException, DAOException {

		ZMQ.Context context = ZMQ.context();
		ZMQ.Socket socket = context.socket(ZMQ.REQ);

		try (CheckpointController requestor = CheckpointController.create()) {
			// get server address and port
			String address = requestor.checkPointWait(CONTEXT_CHECKPOINT_ADDRESS, DEFAULT_TIMEOUT);

			log.debug("Server address received: {}", address);

			// connect to the server
			socket.connect(address);


			// count down
			requestor.latchCountDown(CONTEXT_RENDEZVOUS);

			// wait for others
			requestor.checkPointWait(CONTEXT_CHECKPOINT_GO, DEFAULT_TIMEOUT);
			log.debug("CHECKPOINT_GO reached");

			// --------------------------------------------------------------------
			// test
			// --------------------------------------------------------------------
			int messages = PropertyHelper.getNumberOfMessages();

			long start = System.nanoTime();
			for (int msgCount = 0; msgCount < messages; ++msgCount) {
				String msg = String.format("CLIENT: %s, RUN: %d", getId(), msgCount);
				socket.send(msg);

				//do nothing with the reply
				socket.recvStr();
			}

			// print something
			long end = System.nanoTime();
			long elapsed = (end - start);
			log.debug("Test completed in {} ms", elapsed / 1000000);

			final PropertyReader propertyReader = createPropertyReader();
			storeResult(elapsed, messages, propertyReader.getInteger(CURRENT_RUN_KEY, 0));

		} catch (TimeoutException e) {
			throw new TaskException("The client timeout!", e);
		} finally {
			// don't forget to close the connection
			socket.close();
			context.term();

		}
	}

	private void storeResult(long elapsed, int messages, int run) throws DAOException {
		ExampleResult result = results.createResult(ExampleResult.class);

		result.elapsed = elapsed;
		result.messages = messages;
		result.run = run;

		results.persistResult(result, RESULT_GROUP);
	}
}
