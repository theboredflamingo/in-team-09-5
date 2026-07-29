package com.dbtraining.reconx.model;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TradeEventConverter implements AttributeConverter<TradeEvent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(TradeEvent attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialise TradeEvent", e);
        }
    }

    @Override
    public TradeEvent convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, TradeEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot deserialise TradeEvent", e);
        }
    }
}
