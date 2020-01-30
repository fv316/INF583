package spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;
import java.util.Arrays;

public class Essai2 {
	
	public static void main(String[] args) {
		SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("Letter accumulator");
		try (JavaSparkContext sc = new JavaSparkContext(conf)) {
			//List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
			//JavaRDD<Integer> distData = sc.parallelize(data);
			JavaRDD<String> distFile = sc.textFile(args[0]);
			JavaPairRDD<String, String> counts = distFile
					.flatMap(s -> Arrays.asList(s.split(" ")).iterator())
					.mapToPair(word -> new Tuple2<String, String>(word, String.valueOf(word.charAt(0))))
					.reduceByKey((a, b) -> a + b);
			counts.saveAsTextFile(args[1]);
		}
	}
}
