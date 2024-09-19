package org.example;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Parser;
import org.apache.flink.statefun.sdk.java.types.Type;

public final class MyCustomTypes {

    /**
     * See {@link TypeUtils#createJsonType(String, Class)} for the recommended way of creating a
     * {@link Type} for your JSON messages.
     */
    public static final Type<UserLogin> USER_LOGIN_JSON_TYPE =
            TypeUtils.createJsonType("test.types", UserLogin.class);

}
