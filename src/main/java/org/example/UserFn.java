
package org.example;

import org.apache.flink.statefun.sdk.java.*;
import org.apache.flink.statefun.sdk.java.io.KafkaEgressMessage;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

final class UserFn implements StatefulFunction {

    private static final ValueSpec<Integer> SEEN_COUNT = ValueSpec.named("seen_count").withIntType();
    private static final ValueSpec<Long> SEEN_TIMESTAMP_MS =
            ValueSpec.named("seen_timestamp_ms").withLongType();

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
        int count = context.storage().get(SEEN_COUNT).orElse(0); // Current seen count

        if (message.is(MyCustomTypes.USER_LOGIN_JSON_TYPE)) {
            System.out.println("Message is user login type");
            UserLogin login = message.as(MyCustomTypes.USER_LOGIN_JSON_TYPE);
            System.out.println("The user id is " + login.getUserId());
            System.out.println("The user name is " + login.getUserName());
            System.out.println("The interface is " + login.getLoginType());

            // Call the local lambda function to double the login count
            LocalLambdaFunction lambdaFunction = new LocalLambdaFunction();
            int doubledLoginCount = lambdaFunction.processUserLogin(count); // Pass the current count
            System.out.println("Doubled Login Count: " + doubledLoginCount);

            final long nowMs = System.currentTimeMillis();
            String greetings = String.format("Hello %s for %d time", login.getUserName(), count);

            // Update state
            context.storage().set(SEEN_COUNT, count + 1); // Increment the count
            context.storage().set(SEEN_TIMESTAMP_MS, nowMs);
            System.out.println("Hello world");

            // Send message to Kafka
            context.send(
                    KafkaEgressMessage.forEgress(KAFKA_EGRESS)
                            .withTopic("greetings")
                            .withUtf8Key(login.getUserId())
                            .withUtf8Value(greetings)
                            .build());

            // Insert into PostgreSQL
            try {
                insertIntoDatabase(login.getUserId(), login.getUserName(), login.getLoginType());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return context.done();
    }

    /**
     * Inserts data into PostgreSQL database.
     */
    public void insertIntoDatabase(String userId, String userName, String loginType) throws SQLException {
        String insertQuery = "INSERT INTO user_login (user_id, user_name, login_type) VALUES (?, ?, ?)";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, userName);
            preparedStatement.setString(3, loginType);

            preparedStatement.executeUpdate();
            System.out.println("Data inserted into PostgreSQL successfully.");
        }
    }

    public void restFunction() {
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
}
