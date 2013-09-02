package cz.cuni.mff.d3s.been.task;

import java.util.concurrent.TimeUnit;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.socketworks.twoway.RequestException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Demonstrates working with checkpoints in different threads from the same task.
 *
 * Even though it is possible, it is not recommended.
 *
 * @author Martin Sixta
 */
public class ExampleSyncTask extends Task {

	private static final Logger log = LoggerFactory.getLogger(ExampleSyncTask.class);


	private static final String SYNC_CHECKPOINT = "been.task.sync2.checkpoint";
	private static final String WAIT_PROPERTY = "been.task.sync2.wait";
	private static final int WAIT_SECONDS;

	static {
		String wait = System.getenv(WAIT_PROPERTY);
		// does not handle NumberFormatException ...
		WAIT_SECONDS = wait == null ? 5 : Integer.valueOf(wait);
	}

	@Override
	public void run(String[] args) {
		Thread setter = new Thread() {
			@Override
			public void run() {
				try (CheckpointController requestor = CheckpointController.create()) {
					Thread.sleep(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));
					requestor.checkPointSet(SYNC_CHECKPOINT, "FTW");
				} catch (InterruptedException | RequestException | MessagingException e) {
					log.error("Exception in the setter thread", e);
				}
			}
		};

		Thread waiter = new Thread() {
			@Override
			public void run() {

				try (CheckpointController requestor = CheckpointController.create()) {
					requestor.checkPointWait(SYNC_CHECKPOINT);
				} catch (RequestException | MessagingException e) {
					log.error("Exception in the getter thread", e);
				}

			}
		};

		log.info("Starting two threads. One will wait on the other to set checkpoint");
		waiter.start();
		setter.start();

		try {
			waiter.join();
			setter.join();
		} catch (InterruptedException e) {
			log.error("Exception while waiting for threads ti finish");
		}

		log.info("Done");

	}
}
