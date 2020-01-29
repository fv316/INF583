
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
/* Warning this is a copy of the code provided in the tutorial
 * https://hadoop.apache.org/docs/current/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
 *                 it is not a file that can be directly compiled
 *                 */
import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class RatingAverage {

  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
	private final String DELIMITER = ",";
    private IntWritable rating = new IntWritable();


    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {

		String[] tokens = value.toString().split(DELIMITER);
		word.set(tokens[1]);	
		rating.set(Integer.parseInt(tokens[2]));
        context.write(word, rating);
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,FloatWritable> {
    private FloatWritable result = new FloatWritable();

    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      float sum = 0;
      float total = 0;
      for (IntWritable val : values) {
        sum += val.get();
        total += 1;
      }
      result.set(sum/total);
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "rating average");
    job.setJarByClass(RatingAverage.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}