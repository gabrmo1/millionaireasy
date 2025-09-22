package br.com.bot_mexc.configs.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class StringCryptoConverter implements AttributeConverter<String, String> {

    private static StringEncryptor stringEncryptor;

    @Autowired
    public void setStringEncryptor(StringEncryptor stringEncryptor) {
        StringCryptoConverter.stringEncryptor = stringEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return stringEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return stringEncryptor.decrypt(dbData);
    }
}