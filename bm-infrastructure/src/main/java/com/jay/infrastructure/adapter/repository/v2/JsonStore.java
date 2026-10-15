package com.jay.infrastructure.adapter.repository.v2;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
public final class JsonStore {
 private static final ObjectMapper MAPPER=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 private JsonStore(){}
 public static String write(Object value){try{return MAPPER.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Cannot serialize receipt",e);}}
 public static <T>T read(String json,Class<T> type){try{return MAPPER.readValue(json,type);}catch(Exception e){throw new IllegalStateException("Invalid persisted receipt",e);}}
}

