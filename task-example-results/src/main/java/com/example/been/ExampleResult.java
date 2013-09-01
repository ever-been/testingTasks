package com.example.been;

import cz.cuni.mff.d3s.been.results.Result;

/**
 * Example of a task result.
 *
 * @author Martin Sixta
 */
public class ExampleResult extends Result {

	/**
	 * Results must be stored in a "container" which is defined by a Group ID.
	 *
	 * The Group ID is a string which should describe the type of related results.
	 *
	 */
	 static final String GROUP_ID = "example-data";

	/**
	 * All fields will be serialized/deserialized.
	 */
	private int data;

	/**
	 * Results MUST have non-parametric constructor.
	 */
	public ExampleResult() {

	}

	/**
	 * It is not necessary to use getters - all fields will stored.
	 *
	 * @return example data
	 */
	public int getData() {
		return data;
	}

	/**
	 * It is not necessary to use setter.

	 * @param data example data
	 */
	public void setData(int data) {
		this.data = data;
	}
}
