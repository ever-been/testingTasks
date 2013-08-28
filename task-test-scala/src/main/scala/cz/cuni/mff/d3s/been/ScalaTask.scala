package cz.cuni.mff.d3s.been

import cz.cuni.mff.d3s.been.taskapi.Task
import org.slf4j.LoggerFactory

class ScalaTask extends Task {
	val log = LoggerFactory.getLogger(getClass);

	def run(args: Array[String]) {
		log.info("Logging from Scala based task. WOW!")
	}
}