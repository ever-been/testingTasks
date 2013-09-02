package com.example.been;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.socketworks.twoway.RequestException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;


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

	static final String CHECK_POINT = "checkpoint";

	static final String CHECK_POINT_VALUE = "42";

	static final String LATCH = "example-latch";

	@Override
	public void run(String[] strings) throws TaskException, MessagingException, DAOException {

		try (CheckpointController controller = CheckpointController.create()) {
			log.info("Trying to get checkpoint ... ");
			String result = controller.checkPointGet(CHECK_POINT);

			log.info("Got {}", result);

			log.info("Lets try setting the value of the checkpoint first to {}", CHECK_POINT_VALUE);

			controller.checkPointSet(CHECK_POINT, CHECK_POINT_VALUE);

			result = controller.checkPointGet(CHECK_POINT);

			log.info("Got {}", result);

			log.info("Trying to count down Latch '{}'", LATCH);

			try {
				controller.latchCountDown(LATCH);
			} catch (RequestException e) {
				log.error("Cannot count down", e);
			}

			log.info("Lets try setting the value of the latch first to 2");

			controller.latchSet(LATCH, 2);

			log.info("Lets try to count down again");
			controller.latchCountDown(LATCH);

			log.info("The latch is now at 1, lets try to wait for it ...");


			try {
				controller.latchWait(LATCH, 1000);
			} catch (TimeoutException e) {
				log.error("Waiting for the latch timed out", e);
			}

			log.info("We must count down one more time");
			controller.latchCountDown(LATCH);

			try {
				controller.latchWait(LATCH, 1000);
			} catch (TimeoutException e) {
				log.error("Waiting for the latch timed out", e);
			}

			log.info("Of course this makes sense mainly for inter task synchronization");
		}
	}
}
