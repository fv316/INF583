package twitterapp;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import twitter4j.Status;

public class TwitterApp {

  public static void main(String[] args) {

  
  	Logger.getLogger("org").setLevel(Level.ERROR);
  	Logger.getLogger("akka").setLevel(Level.ERROR);
  	
    
    // Set the system properties so that Twitter4j library used by Twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", "");
    System.setProperty("twitter4j.oauth.consumerSecret", "");
    System.setProperty("twitter4j.oauth.accessToken", "-");
    System.setProperty("twitter4j.oauth.accessTokenSecret", "");

    SparkConf sparkConf = new SparkConf().setMaster("local[*]").setAppName("TwitterApp");
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(60));
    
    String[] filters = {"impeachment process", "breaking"};        
    JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);

	JavaDStream<String> infoTweets = stream.map(s ->s.getUser().getName() + " says at "+  s.getCreatedAt() + " the following: "+ s.getText().replace('\n', ' ')); 
    infoTweets.foreachRDD( x-> { x.collect().stream().forEach(n-> System.out.println(n));});
    
    jssc.start();
    try {jssc.awaitTermination();} catch (InterruptedException e) {e.printStackTrace();}
  }
}
