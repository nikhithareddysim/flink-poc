package org.example;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.flink.statefun.sdk.java.StatefulFunctions;
import org.apache.flink.statefun.sdk.java.handler.RequestReplyHandler;
import org.apache.flink.statefun.sdk.java.slice.Slice;
import org.apache.flink.statefun.sdk.java.slice.Slices;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String ...args)
    {
        final StatefulFunctions functions = new StatefulFunctions();
        functions.withStatefulFunction(UserFn.SPEC);

        // ... and build a request-reply handler for the registered functions, which understands how to
        // decode invocation requests dispatched from StateFun / encode side-effects (e.g. state storage
        // updates, or invoking other functions) as responses to be handled by StateFun.
        final RequestReplyHandler requestReplyHandler = functions.requestReplyHandler();

        // Use the request-reply handler along with your favorite HTTP web server framework
        // to serve the functions!
        final Undertow httpServer =
                Undertow.builder()
                        .addHttpListener(1108, "localhost")
                        .setHandler(new UndertowHttpHandler(requestReplyHandler))
                        .build();
        httpServer.start();
    }
    static final class UndertowHttpHandler implements HttpHandler {
        private final RequestReplyHandler handler;

        UndertowHttpHandler(RequestReplyHandler handler) {
            this.handler = Objects.requireNonNull(handler);
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) {
            exchange.getRequestReceiver().receiveFullBytes(this::onRequestBody);
        }

        private void onRequestBody(HttpServerExchange exchange, byte[] requestBytes) {
            exchange.dispatch();
            CompletableFuture<Slice> future = handler.handle(Slices.wrap(requestBytes));
            future.whenComplete((response, exception) -> onComplete(exchange, response, exception));
        }

        private void onComplete(HttpServerExchange exchange, Slice responseBytes, Throwable ex) {
            if (ex != null) {
                ex.printStackTrace(System.out);
                exchange.getResponseHeaders().put(Headers.STATUS, 500);
                exchange.endExchange();
                return;
            }
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
            exchange.getResponseSender().send(responseBytes.asReadOnlyByteBuffer());
        }
    }

}
