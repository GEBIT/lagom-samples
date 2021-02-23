//
// GrpcAppTopic.java
//
// Copyright (C) 2020
// GEBIT Solutions GmbH,
// Berlin, Duesseldorf, Stuttgart (Germany)
// All rights reserved.
//
package com.example.hello.impl;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lightbend.lagom.javadsl.pubsub.PubSubRef;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import example.myapp.helloworld.grpc.StatusResponse;

/**
 * GrpcAppTopic implementation.
 * 
 * @author CAnderwald
 */
public class GrpcStatusTopic {

	/**
	 * The logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(GrpcStatusTopic.class);

	/**
	 * Qualifier for the topic.
	 */
	private static final String TOPIC_QUALIFIER = "topic-global";

	/**
	 * the lagom pub sub registry.
	 */
	private final PubSubRegistry pubSub;

	/**
	 * C' tor.
	 * 
	 * @param aPubSub the pubsub registry
	 */
	@Inject
	public GrpcStatusTopic(PubSubRegistry aPubSub) {
		this.pubSub = aPubSub;
	}

	/**
	 * Provide a subscription for an checkout receipt.
	 */
	public Source<StatusResponse, NotUsed> subscriber() {
		Source<StatusResponse, NotUsed> tSource = refFor().subscriber();
		return tSource;
	}

	/**
	 * Publish a status.
	 */
	public CompletionStage<Done> publish(String aStatus) {
		PubSubRef<StatusResponse> tRef = refFor();

		return tRef.hasAnySubscribers().thenApply(hasAnySubscribers -> {
			if (hasAnySubscribers) {
				tRef.publish(StatusResponse.newBuilder().setStatus(aStatus).build());
			} else {
				LOGGER.info("no active subscriptions");
			}
			return Done.getInstance();
		});
	}

	private PubSubRef<StatusResponse> refFor() {
		return pubSub.refFor(TopicId.of(StatusResponse.class, TOPIC_QUALIFIER));
	}

}
