package com.example.been;

import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;

import cz.cuni.mff.d3s.been.taskapi.Task;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hello example!
 *
 * Shows minimal Java based BEEN task, along with logging support.
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
		System.out.println("Hello!");
		System.err.println("Aloha!");

		log.trace("Mae govannen!");

		log.debug("السلام عليكم");
		log.info("Salut!");
		log.warn("你好");
		log.error("שלום");


	}
}
