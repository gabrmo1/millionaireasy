package br.com.bot_mexc.shared.utils;

import lombok.experimental.UtilityClass;

import java.time.Instant;

@UtilityClass
public class DateUtils {

    public static Instant agora() {
        return Instant.now();
    }

}
