package com.example.hello.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import example.myapp.helloworld.grpc.AbstractGreeterServiceRouter;
import example.myapp.helloworld.grpc.HelloReply;
import example.myapp.helloworld.grpc.HelloRequest;
import example.myapp.helloworld.grpc.StatusRequest;
import example.myapp.helloworld.grpc.StatusResponse;
import scala.concurrent.ExecutionContext;

@Singleton
public class HelloGrpcServiceImpl extends AbstractGreeterServiceRouter {

	/**
	 * Actor System.
	 */
	private ActorSystem actorSystem = null;

	/**
	 * ExecutionContext.
	 */
	private ExecutionContext executionContext = null;

	/**
	 * GrpcAppTopic.
	 */
	@Inject
	private GrpcStatusTopic grpcStatusTopic;

	/**
	 * Boolean flag of status tick source has been started.
	 */
	private static boolean statusTickStarted = false;

	@Inject
	public HelloGrpcServiceImpl(ActorSystem sys, Materializer mat, ExecutionContext anExecutionContext) {
		super(mat, sys);
		actorSystem = sys;
		executionContext = anExecutionContext;
	}

	@Override
	public CompletionStage<HelloReply> sayHello(HelloRequest in) {
		HelloReply reply = HelloReply.newBuilder().setMessage("Hi " + in.getName() + " (gRPC)").build();
		return CompletableFuture.completedFuture(reply);
	}

	@Override
	public Source<StatusResponse, NotUsed> statusStream(StatusRequest aIn) {
		System.out.println("statusStream invoked");
		if (!statusTickStarted) {
			System.out.println("starting scheduler");
			actorSystem.scheduler()
					.scheduleWithFixedDelay(Duration.ofSeconds(5),
							// setting the following value to 90 will cause the connection error problem at the clients
							Duration.ofSeconds(90),
							this::publishStatus,
							executionContext);

			statusTickStarted = true;
			System.out.println("scheduler started");
		}

		Source<StatusResponse, NotUsed> tPubSubSubscriber = grpcStatusTopic.subscriber();
		return tPubSubSubscriber;
	}

	public void publishStatus() {
		System.out.println("publishStatus invoked");
		grpcStatusTopic.publish(Instant.now().toString());
	}

}
