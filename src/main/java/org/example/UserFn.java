package org.example;

import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.example.UserLogin;


import java.io.IOException;
import java.util.concurrent.CompletableFuture;

final class UserFn implements StatefulFunction {

    private static final ValueSpec<Integer> SEEN_COUNT = ValueSpec.named("seen_count").withIntType();
    private static final ValueSpec<Long> SEEN_TIMESTAMP_MS =
            ValueSpec.named("seen_timestamp_ms").withLongType();

    /**
     * Every registered function needs to be associated with an unique {@link TypeName}. A function's
     * typename is used by other functions and ingresses to have messages addressed to them.
     */
    static final TypeName TYPENAME = TypeName.typeNameOf("test.fns", "user");

    static final StatefulFunctionSpec SPEC =
            StatefulFunctionSpec.builder(TYPENAME)
                    .withValueSpecs(SEEN_COUNT, SEEN_TIMESTAMP_MS)
                    .withSupplier(UserFn::new)
                    .build();

    private static final TypeName KAFKA_EGRESS =
            TypeName.typeNameOf("test.io", "user-greetings");

    @Override
    public CompletableFuture<Void> apply(Context context, Message message) {
        int count  = context.storage().get(SEEN_COUNT).orElse(0);
//        System.out.println(count);
        if(message.is(MyCustomTypes.USER_LOGIN_JSON_TYPE)) {
            System.out.println("Message is user login type type");
            UserLogin login = message.as(MyCustomTypes.USER_LOGIN_JSON_TYPE);
            System.out.println("The user id is " + login.getUserId());
            System.out.println("The user name is " + login.getUserName());
            System.out.println("The interface is " + login.getLoginType());
            // System.out.println("The message is " + message.as(MyCustomTypes.USER_LOGIN_JSON_TYPE));

            //  context.storage().set(SEEN_COUNT,count+1);
            final long nowMs = System.currentTimeMillis();
          //  final long lastSeenTimestampMs = context.storage().get(SEEN_TIMESTAMP_MS).orElse(nowMs);
            Integer c = sum(4, 10);
            System.out.println(c);

            /**
             * For making rest calls from stateful function
             * */
        //    restFunction();

            String greetings = String.format("Hello %s for %d time",login.getUserName(),count);

            context.storage().set(SEEN_COUNT, count + 1);
            context.storage().set(SEEN_TIMESTAMP_MS, nowMs);
            System.out.println("Hello world");
            context.send(
                    KafkaEgressMessage.forEgress(KAFKA_EGRESS)
                            .withTopic("greetings")
                            .withUtf8Key(login.getUserId())
                            .withUtf8Value(greetings)
                            .build());
        }
        return context.done();
    }

    public void restFunction()
    {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut httpPost = new HttpPut("http://localhost:7071/process/");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    System.out.println("Response: " + responseEntity.getContent().toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer sum(int a, int b)
    {
        return a+b;
    }

}
