package com.example.been.benchmark.server;

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
import java.util.concurrent.TimeoutException;

import static com.example.been.benchmark.common.PropertyHelper.*;

/**
 * @author Martin Sixta
 */
public class ServerTask extends Task {

	/**
	 * logging
	 */
	private static final Logger log = LoggerFactory.getLogger(ServerTask.class);

	private ZMQ.Context context = null;

	ZMQ.Socket socket = null;

	@Override
	public void run(String[] args) throws TaskException, MessagingException, DAOException {


		final String address = startServer();


		try (CheckpointController checkpoints = CheckpointController.create()) {
			// set count down latch, this must be done before address checkpoint!
			checkpoints.latchSet(CONTEXT_RENDEZVOUS, getNumberOfClients());

			// set checkpoint
			checkpoints.checkPointSet(CONTEXT_CHECKPOINT_ADDRESS, address);

			// wait for all clients
			checkpoints.latchWait(CONTEXT_RENDEZVOUS, DEFAULT_TIMEOUT);

			checkpoints.checkPointSet(CONTEXT_CHECKPOINT_GO, "go");

			int totalNumberOfConnections = getTotalNumberOfConnections();

			long start = System.nanoTime();
			for (int i = 0; i < totalNumberOfConnections; ++i) {
				String msg = socket.recvStr();

				// handle server timeout
				if (msg == null) {
					log.error("Server timeout!");
					break;
				}

				socket.send("OK: " + msg);
			}

			long end = System.nanoTime();
			log.debug("Server completed test in {} ms ", (end - start) / 1000000);

		} catch (TimeoutException e) {
			throw new TaskException("Server timeout!");
		} finally {
			// don't forget to close the connection
			socket.close();
			context.term();
		}

	}

	private String startServer() throws TaskException {


		final String hostName = getHostName();
		final String address = String.format("tcp://%s", hostName);

		try {
			context = ZMQ.context();

			socket = context.socket(ZMQ.REP);
			socket.setReceiveTimeOut(DEFAULT_TIMEOUT);
			socket.setSendTimeOut(0); // will not wait
			int port = socket.bindToRandomPort(address);

			return String.format("tcp://%s:%d", hostName, port);
		} catch (Exception e) {
			throw new TaskException("Cannot start the server!");
		}

	}

	private String getHostName() throws TaskException {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new TaskException("Cannot obtain host name!");
		}

	}
}
