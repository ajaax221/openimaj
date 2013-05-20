package org.openimaj.demos.sandbox.ml.linear.learner.stream;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openimaj.ml.linear.evaluation.SumLossEvaluator;
import org.openimaj.ml.linear.learner.BilinearLearnerParameters;
import org.openimaj.ml.linear.learner.init.FirstValueInitStrat;
import org.openimaj.ml.linear.learner.init.SingleValueInitStrat;
import org.openimaj.ml.linear.learner.init.SparseZerosInitStrategy;
import org.openimaj.tools.twitter.modes.preprocessing.StopwordMode;
import org.openimaj.twitter.USMFStatus;
import org.openimaj.util.function.Operation;
import org.openimaj.util.pair.IndependentPair;
import org.openimaj.util.pair.Pair;
import org.openimaj.util.stream.window.Aggregation;
import org.openimaj.util.stream.window.CombinedAggregationFunction;
import org.openimaj.util.stream.window.WindowAverage;

import com.mongodb.DBObject;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class FinancialMongoStreamLearningExperiment {
	/**
	 * @param args
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws MalformedURLException, IOException {
		BilinearLearnerParameters params = new BilinearLearnerParameters();
		params.put(BilinearLearnerParameters.ETA0_U, 0.02);
		params.put(BilinearLearnerParameters.ETA0_W, 0.02);
		params.put(BilinearLearnerParameters.LAMBDA, 0.001);
		params.put(BilinearLearnerParameters.BICONVEX_TOL, 0.01);
		params.put(BilinearLearnerParameters.BICONVEX_MAXITER, 10);
		params.put(BilinearLearnerParameters.BIAS, true);
		params.put(BilinearLearnerParameters.ETA0_BIAS, 0.5);
		params.put(BilinearLearnerParameters.WINITSTRAT, new SingleValueInitStrat(0.1));
		params.put(BilinearLearnerParameters.UINITSTRAT, new SparseZerosInitStrategy());
		FirstValueInitStrat biasInitStrat = new FirstValueInitStrat();
		params.put(BilinearLearnerParameters.BIASINITSTRAT, biasInitStrat);

		List<ServerAddress> serverList = Arrays.asList(
				new ServerAddress("rumi", 27017),
				new ServerAddress("hafez", 27017)
				);

		// The combined stream
		new MongoDBQueryStream<Aggregation<IndependentPair<List<USMFStatus>, List<Map<String, Double>>>,IndependentPair<Long,Long>>>(serverList) {

			@Override
			public String getCollectionName() {
				return "streamapi_yahoo";
			}

			@Override
			public String getDBName() {
				return "twitterticker";
			}

			@Override
			@SuppressWarnings("unchecked")
			public Aggregation<IndependentPair<List<USMFStatus>, List<Map<String, Double>>>,IndependentPair<Long,Long>> constructObjects(DBObject next) {
				List<Map<String, Double>> ticks = (List<Map<String, Double>>) next.get("tickers");
				List<USMFStatus> tweets = new ArrayList<USMFStatus>();
				List<Object> objl = (List<Object>) next.get("tweets");
				for (Object object : objl) {
					USMFStatus status = new USMFStatus();
					status.fillFromString(JSON.serialize(object));
					tweets.add(status);
				}
				Long timestamp = (Long) next.get("timestamp");
				IndependentPair<List<USMFStatus>, List<Map<String, Double>>> ret = IndependentPair.pair(tweets, ticks);
				return new Aggregation<IndependentPair<List<USMFStatus>,List<Map<String,Double>>>, IndependentPair<Long,Long>>(ret, IndependentPair.pair(timestamp,timestamp));
			}
		}
				.map(
					new CombinedAggregationFunction<
						List<USMFStatus>, Map<String,Map<String,Double>>,
						List<Map<String,Double>>, Map<String,Double>,
						Long, Long>(
							new USMFStatusUserWordScore<Aggregation<List<USMFStatus>,Long>>(new StopwordMode()),
							new WindowAverage<Aggregation<List<Map<String,Double>>,Long>, Long>()
					)
				)
				.transform(new StockPriceAggregator(0.0001))
				.map(
						new IncrementalLearnerWorldSelectingEvaluator(
								new SumLossEvaluator(),
								new IncrementalLearnerFunction(params)
						)
				)
				.forEach(new Operation<ModelStats>() {

					@Override
					public void perform(ModelStats object) {
						System.out.println("Loss: " + object.score);
						System.out.println("Important words: ");
						for (String task : object.importantWords.keySet()) {
							Pair<Double> minmax = object.taskMinMax.get(task);
							System.out.printf("... %s (%1.4f->%1.4f) %s\n",
									task,
									minmax.firstObject(),
									minmax.secondObject(),
									object.importantWords.get(task)
									);
						}
						System.out.println("Correct Y: \n" + object.correctY);
						System.out.println("Estimated Y: \n" + object.estimatedY);
					}
				});

	}
}