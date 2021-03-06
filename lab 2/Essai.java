package spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;
import java.util.Arrays;
import java.util.List;

public class Essai {
	public static void main(String[] args) {
		SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("Word counter");
		try (JavaSparkContext sc = new JavaSparkContext(conf)) {
			//List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
			//JavaRDD<Integer> distData = sc.parallelize(data);
			JavaRDD<String> distFile = sc.textFile(args[0]);
			JavaPairRDD<String, Integer> counts = distFile
					.flatMap(s -> Arrays.asList(s.split(" ")).iterator())
					.mapToPair(word -> new Tuple2<String, Integer>(word, 1))
					.reduceByKey((a, b) -> a + b);
			counts.saveAsTextFile(args[1]);
		}
	}
}