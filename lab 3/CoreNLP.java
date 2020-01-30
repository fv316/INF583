package twitterapp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import scala.Tuple2;
import twitter4j.Status;

import org.apache.avro.generic.GenericData.Array;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.twitter.TwitterUtils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.util.Properties;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.simple.*;

public class CoreNLP {
	static StanfordCoreNLP pipeline = null;
	public static StanfordCoreNLP getOrCreatePipeline ()
	{
		if (pipeline == null)
			{ 
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse,ner,sentiment,depparse,natlog,openie");
			pipeline = new StanfordCoreNLP(props);
			}
		return pipeline;
	}
	
	public static void main(String[] args) {

		 
	Logger.getLogger("org").setLevel(Level.ERROR);
  	Logger.getLogger("akka").setLevel(Level.ERROR);
  	
    
    // Set the system properties so that Twitter4j library used by Twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", "");
    System.setProperty("twitter4j.oauth.consumerSecret", "");
    System.setProperty("twitter4j.oauth.accessToken", "-");
    System.setProperty("twitter4j.oauth.accessTokenSecret", "");

    SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("TwitterApp");
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds((2)));
    
    String[] filters = {"impeachment process", "breaking"};        
    JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, filters);
    // Note: the sentiment is a score scale of 0 = very negative, 1 = negative, 2 = neutral, 3 = positive, and 4 = very positive.
    // the type of an entity is a string, which can be "PERSON", "ORGANIZATION", "LOCATION"
    JavaPairDStream<String, Double> personSent = stream.flatMapToPair(s ->
    {
    	CoreDocument doc = new CoreDocument(s.getText()); 
    	getOrCreatePipeline().annotate(doc);
        double overall_sent = 0.0;
        for (CoreMap sentence : doc.annotation().get(CoreAnnotations.SentencesAnnotation.class)) {
            Tree tree = sentence.get(SentimentAnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
            overall_sent +=sentiment;
        }
        overall_sent = overall_sent/doc.sentences().size();
    	ArrayList<Tuple2<String, Double>> mentions = new ArrayList<Tuple2<String, Double>>();
        for (CoreEntityMention em : doc.entityMentions())
        	if(em.entityType().equals("PERSON"))
        		mentions.add(new Tuple2<>(em.text(), overall_sent));
        return mentions.iterator();
    });; 
    
       
    personSent.foreachRDD( x-> { x.collect().stream().forEach(n-> System.out.println(n._1 + " " + n._2.toString()));});
	
    // How can we compute the average sentiment? Note that the average of more than two numbers is not an associative operation.

    
    JavaPairDStream<String, Integer> triplesOpenIE = stream.flatMapToPair(s ->
    {
    	CoreDocument doc = new CoreDocument(s.getText()); 
    	getOrCreatePipeline().annotate(doc);
    	ArrayList<Tuple2<String, Integer>> triplesArray = new ArrayList<Tuple2<String, Integer>>();
    	for (CoreMap sentence : doc.annotation().get(CoreAnnotations.SentencesAnnotation.class)) 
    	{  
    	Collection<RelationTriple> triples =
    		          sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
        
    	for (RelationTriple triple : triples) 
            triplesArray.add(new Tuple2<>(triple.subjectLemmaGloss() + "\t" +
                    triple.relationLemmaGloss() + "\t" +
                    triple.objectLemmaGloss(), 1));
    	}
        return triplesArray.iterator();
    });; 
    // we now need to proceed for exercise 3 as for the hashtags example given in the TP
    triplesOpenIE.foreachRDD( x-> { x.collect().stream().forEach(n-> System.out.println(n._1));});

    jssc.start();
    try {jssc.awaitTermination();} catch (InterruptedException e) {e.printStackTrace();}
	 }
}
