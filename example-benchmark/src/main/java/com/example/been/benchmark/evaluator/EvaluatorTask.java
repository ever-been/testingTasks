package com.example.been.benchmark.evaluator;

import com.example.been.benchmark.common.ExampleResult;
import com.example.been.benchmark.common.PropertyHelper;
import cz.cuni.mff.d3s.been.core.persistence.EntityID;
import cz.cuni.mff.d3s.been.evaluators.EvaluatorResult;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.persistence.Query;
import cz.cuni.mff.d3s.been.persistence.QueryBuilder;
import cz.cuni.mff.d3s.been.taskapi.Evaluator;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This task will evaluate data gathered from the example benchmark.
 * <p/>
 * The output is a png file with a chart.
 *
 * @author Martin Sixta
 */
public class EvaluatorTask extends Evaluator {
	private static final Logger log = LoggerFactory.getLogger(EvaluatorTask.class);

	@Override
	public EvaluatorResult evaluate() throws DAOException, TaskException {


		// Entity ID of results
		final EntityID eid = new EntityID().withKind("result").withGroup(PropertyHelper.RESULT_GROUP);

		try {

			// figure out for which benchmark generate the chart
			String benchmarkId = getTaskProperty("evaluator.benchmark.id");

			if (benchmarkId == null || benchmarkId.trim().isEmpty()) {
				log.debug("Using current benchmarkId to fetch results");
				benchmarkId = getBenchmarkId();
			}

			// create query to fetch data with
			Query query = new QueryBuilder().on(eid).with("benchmarkId", benchmarkId).fetch();

			// fetch data from Object Repository
			final Collection<ExampleResult> clientResults = results.query(query, ExampleResult.class);

			// any data returned?
			if (clientResults.size() == 0) {
				String msg = String.format("Found no results for benchmark '%s'", benchmarkId);
				throw new TaskException(msg);
			}

			// Group results by contexts
			Map<Integer, List<Double>> grouped = new HashMap<>();

			// List of runs
			Set<Integer> runs = new TreeSet<>();

			for (ExampleResult result : clientResults) {
				Integer run = result.run;
				if (!grouped.containsKey(run)) {
					grouped.put(run, new ArrayList<Double>());
				}

				// must use double because of the JFreeChart library
				double elapsed = TimeUnit.MILLISECONDS.convert(result.elapsed, TimeUnit.NANOSECONDS);
				grouped.get(run).add(elapsed);
				runs.add(run);
			}


			String yCaption = "Time (ms)";

			int width = 800;
			int height = 600;

			BufferedImage image = generateChart(grouped, runs, yCaption, width, height);

			ImageIO.write(image, "png", new File("out.png"));

			EvaluatorResult er = new EvaluatorResult();
			er.setBenchmarkId(getBenchmarkId());
			er.setFilename("example-benchmark-chart.png");
			er.setTimestamp(System.currentTimeMillis());
			er.setMimeType(EvaluatorResult.MIME_TYPE_IMAGE_PNG);
			er.setData(Files.readAllBytes(Paths.get("out.png")));
			er.setId(getId());

			return er;

		} catch (IllegalStateException e) {
			throw new TaskException("Cannot build a query to fetch results", e);
		} catch (IOException e) {
			throw new TaskException("Cannot generate results plot!", e);
		}

	}

	public BufferedImage generateChart(Map<Integer, List<Double>> data, Set<Integer> runs, String yCaption,
									   int width, int height) {
		// create dataset
		YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		YIntervalSeries s1 = new YIntervalSeries(yCaption);

		int i = 0;

		for (Integer run : runs) {

			DescriptiveStatistics stats = getStats(data.get(run));

			double mean = stats.getMean();
			double std = stats.getStandardDeviation();

			s1.add(i++, mean, mean - 1.96 * std, mean + 1.96 * std);

		}

		dataset.addSeries(s1);


		final String[] stringsArray = new String[runs.size()];
		int k = 0;
		for (Integer run : runs) {

			stringsArray[k++] = StringUtils.abbreviate(run.toString(), 10);


		}

		ValueAxis xAxis = new SymbolAxis("Iteration", stringsArray);

		NumberAxis yAxis = new NumberAxis("Processing time (ms)");
		yAxis.setAutoRangeStickyZero(false);
		XYErrorRenderer renderer = new XYErrorRenderer();
		renderer.setBaseLinesVisible(true);
		renderer.setSeriesStroke(0, new BasicStroke(3.0f));
		renderer.setBaseShapesVisible(false);
		renderer.setErrorPaint(Color.blue);
		renderer.setErrorStroke(new BasicStroke(1.0f));
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.lightGray);
		JFreeChart chart = new JFreeChart("Average time to send all messages", plot);
		chart.setBackgroundPaint(Color.white);

		// create output image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setBackground(Color.white);
		graphics.clearRect(0, 0, width, height);

		// render
		chart.draw(graphics, new Rectangle2D.Double(0, 0, width, height));

		return image;
	}

	private double[] toPrimitiveArray(List<Double> list) {
		// Stupid Java
		double[] values = new double[list.size()];

		for (int j = 0; j < list.size(); ++j) {
			values[j] = list.get(j);
		}

		return values;
	}

	private DescriptiveStatistics getStats(List<Double> data) {

		double[] values = toPrimitiveArray(data);

		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = new DescriptiveStatistics();

		// Add the data from the array
		for (double value : values) {
			stats.addValue(value);
		}
		return stats;

	}
}
