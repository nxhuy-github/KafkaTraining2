package com.nxhuy.kafka.training.KafkaTraining2;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.*;
import twitter4j.conf.*;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import kafka.producer.KeyedMessage;

/**
 * Hello world!
 *
 */
public class KafkaTwitterProducer 
{
    public static void main( String[] args )
    {
    	final LinkedBlockingQueue<Status> queue = new LinkedBlockingQueue<Status>(1000);
        
    	if(args.length < 4) {
    		System.out.println("");
    		return;
    	}
    	
    	String consumerKey = args[0].toString();
    	String consumerSecret = args[1].toString();
    	String accessToken = args[2].toString();
    	String accessSecret = args[3].toString();
    	String topicName = args[4].toString();
    	String[] arguments = args.clone();
    	String[] keyWords = Arrays.copyOfRange(arguments, 5, arguments.length);
    	
    	ConfigurationBuilder cb = new ConfigurationBuilder();
    	cb.setDebugEnabled(true).setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessSecret).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret);
    	
    	TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    	StatusListener listener = new StatusListener() {

			public void onException(Exception arg0) {
				arg0.printStackTrace();
			}

			public void onDeletionNotice(StatusDeletionNotice arg0) {
				System.out.println("Got a status deletion notice id: " + arg0.getStatusId());
			}

			public void onScrubGeo(long arg0, long arg1) {
				System.out.println("Gor a srub_geo event userID: " + arg0 + ", upToStatusID: " + arg1);
			}

			public void onStallWarning(StallWarning arg0) {
				System.out.println("Got stall warning: " + arg0);
			}

			public void onStatus(Status arg0) {
				queue.offer(arg0);
			}

			public void onTrackLimitationNotice(int arg0) {
				System.out.println("Got track limitation notice: " + arg0);
			}
    		
    	};
    	
    	twitterStream.addListener(listener);
    	
    	FilterQuery query = new FilterQuery().track(keyWords);
    	twitterStream.filter(query);
    	
    	Properties props = new Properties();
    	props.put("metadata.broker.list", "localhost:9092");
		props.put("bootstrap.servers", "localhost:9092");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		KafkaProducer<String, String> producer = new KafkaProducer(props);
		int i = 0;
		int j = 0;
		
		while(true) {
			Status ret = queue.poll();
			
			if(ret == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				for(HashtagEntity hashtag : ret.getHashtagEntities()) {
					System.out.println("Tweet: " + ret);
					System.out.println("Hashtag: " + hashtag.getText());
					
					producer.send(new ProducerRecord<String, String>(topicName, Integer.toString(j++), ret.getText()));
				}
			}
		}
    }
}
