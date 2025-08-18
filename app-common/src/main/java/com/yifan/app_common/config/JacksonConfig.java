package com.yifan.app_common.config;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

@Configuration
public class JacksonConfig {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
            builder.serializers(new LocalDateTimeSerializer(formatter));

            builder.serializerByType(Long.class, new JsonSerializer<Long>() {
                @Override
                public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    if (value != null) {
                        gen.writeString(value.toString());
                    } else {
                        gen.writeNull();
                    }
                }
            });
        };
    }
}
