package com.example.been.benchmark.client;

import com.example.been.benchmark.PropertyHelper;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.jeromq.ZMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.been.benchmark.PropertyHelper.*;

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
			String address = requestor.checkPointWait(CONTEXT_CHECKPOINT_ADDRESS);

			// connect to the server

			socket.connect(address);


			// count down
			requestor.latchCountDown(CONTEXT_RENDEZVOUS);

			// wait for others
			requestor.checkPointWait(CONTEXT_CHECKPOINT_GO);
			log.info("CHECKPOINT_GO reached");

			// --------------------------------------------------------------------
			// test
			// --------------------------------------------------------------------
			int messages = PropertyHelper.getNumberOfMessages();

			long start = System.nanoTime();
			for (int run = 0; run < messages; ++run) {
				String msg = String.format("CLIENT: %s, RUN: %d", getId(), run);
				socket.send(msg);

				//do nothing with the reply
				socket.recvStr();
			}

			// print something
			long end = System.nanoTime();
			log.info("Test completed in {} ms", (end - start) / 1000000);

		} finally {
			// don't forget to close the connection
			socket.close();
			context.term();

		}
	}
}
