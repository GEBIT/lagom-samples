package com.example.helloproxy.impl;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.bind;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.example.hello.api.HelloService;
import com.example.helloproxy.api.HelloProxyService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;

import akka.Done;
import akka.NotUsed;
import akka.grpc.javadsl.AkkaGrpcClient;
import akka.stream.javadsl.Source;
import example.myapp.helloworld.grpc.AkkaGrpcClientModule;
import example.myapp.helloworld.grpc.GreeterServiceClient;
import example.myapp.helloworld.grpc.HelloReply;
import example.myapp.helloworld.grpc.HelloRequest;
import example.myapp.helloworld.grpc.StatusRequest;
import example.myapp.helloworld.grpc.StatusResponse;

public class HelloProxyServiceImplTest {

	private static ServiceTest.TestServer server;

	private static HelloProxyService proxyServiceClient;

	@BeforeClass
	public static void setUp() {
		ServiceTest.Setup setup = defaultSetup().configureBuilder(builder -> builder.disable(AkkaGrpcClientModule.class)
				.overrides(bind(HelloService.class).to(StubHelloService.class))
				.overrides(bind(GreeterServiceClient.class).to(GreeterServiceClientStub.class)));
		server = startServer(setup);
		proxyServiceClient = server.client(HelloProxyService.class);
	}

	@AfterClass
	public static void tearDown() {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Test
	public void helloProxyShouldRoundtripHttpRequests()
			throws InterruptedException, ExecutionException, TimeoutException {
		String msg = proxyServiceClient.proxyViaHttp("Alice").invoke().toCompletableFuture().get(5, SECONDS);
		assertEquals("Hello Alice", msg);
	}

	@Test
	public void helloProxyShouldRoundtripGrpcRequests()
			throws InterruptedException, ExecutionException, TimeoutException {
		String msg = proxyServiceClient.proxyViaGrpc("Steve").invoke().toCompletableFuture().get(5, SECONDS);
		assertEquals("Hi Steve (gRPC)", msg);
	}

	// ---------------------------------------------------------------------------------

	public static class StubHelloService implements HelloService {

		@Override
		public ServiceCall<NotUsed, String> hello(String id) {
			return notUsed -> CompletableFuture.completedFuture("Hello " + id);
		}
	}

	public static class GreeterServiceClientStub extends GreeterServiceClient implements StubbedAkkaGrpcClient {

		@Override
		public CompletionStage<HelloReply> sayHello(HelloRequest in) {
			HelloReply reply = HelloReply.newBuilder().setMessage("Hi " + in.getName() + " (gRPC)").build();
			return CompletableFuture.completedFuture(reply);
		}

		@Override
		public Source<StatusResponse, NotUsed> statusStream(StatusRequest aIn) {
			return null;
		}
	}

	// ------------------------------------------------------------------------
	// ------------------------------------------------------------------------

	public interface StubbedAkkaGrpcClient extends AkkaGrpcClient {

		@Override
		default CompletionStage<Done> close() {
			return null;
		}

		@Override
		default CompletionStage<Done> closed() {
			return null;
		}
	}

}
