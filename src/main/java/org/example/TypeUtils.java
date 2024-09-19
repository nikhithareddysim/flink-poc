package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

final class TypeUtils {

    private TypeUtils() {}

    private static final ObjectMapper JSON_OBJ_MAPPER = new ObjectMapper();

    static <T> Type<T> createJsonType(String typeNamespace, Class<T> jsonClass) {
        return SimpleType.simpleImmutableTypeFrom(
                TypeName.typeNameOf(typeNamespace, jsonClass.getName()),
                JSON_OBJ_MAPPER::writeValueAsBytes,
                bytes -> JSON_OBJ_MAPPER.readValue(bytes, jsonClass));
    }

    static <T extends GeneratedMessageV3> Type<T> createProtobufType(
            String typeNamespace, Descriptors.Descriptor protobufDescriptor, Parser<T> protobufParser) {
        return SimpleType.simpleImmutableTypeFrom(
                TypeName.typeNameOf(typeNamespace, protobufDescriptor.getFullName()),
                T::toByteArray,
                protobufParser::parseFrom);
    }
}

