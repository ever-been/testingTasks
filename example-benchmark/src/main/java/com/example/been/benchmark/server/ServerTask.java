package com.example.been.benchmark.server;

import com.example.been.benchmark.PropertyHelper;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.jeromq.ZMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Martin Sixta
 */
public class ServerTask extends Task {

	/**
	 * logging
	 */
	private static final Logger log = LoggerFactory.getLogger(ServerTask.class);

	@Override
	public void run(String[] args) throws TaskException, MessagingException, DAOException {

		// prepare the server
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new TaskException("Cannot obtain Host name");
		}

		if (host == null) {
			host = "localhost";
		}

		String address = String.format("tcp://%s", host);
		ZMQ.Context context = ZMQ.context();

		ZMQ.Socket socket = context.socket(ZMQ.REP);
		int port = socket.bindToRandomPort(address);

		address = String.format("%s:%d", address, port);


		try (CheckpointController checkpoints = CheckpointController.create()) {
			// set count down latch, this must be done before address checkpoint!
			checkpoints.latchSet(PropertyHelper.CONTEXT_RENDEZVOUS, PropertyHelper.getNumberOfClients());

			// set checkpoint
			checkpoints.checkPointSet(PropertyHelper.CONTEXT_CHECKPOINT_ADDRESS, address);

			// wait for all clients
			checkpoints.latchWait(PropertyHelper.CONTEXT_RENDEZVOUS);

			checkpoints.checkPointSet(PropertyHelper.CONTEXT_CHECKPOINT_GO, "go");

			int totalNumberOfConnections = PropertyHelper.getTotalNumberOfConnections();
			long start = System.nanoTime();
			for (int i = 0; i < totalNumberOfConnections; ++i) {
				String msg = socket.recvStr();
				socket.send("OK: " + msg);
			}

			// print something
			long end = System.nanoTime();
			log.info("Server completed test in {} ms ", (end - start) / 1000000);

		} finally {
			// don't forget to close the connection
			socket.close();
			context.term();
		}

	}
}
