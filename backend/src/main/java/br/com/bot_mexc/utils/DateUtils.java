package br.com.bot_mexc.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;

@UtilityClass
public class DateUtils {

    public static LocalDateTime agora() {
        ZoneId zonaBrasil = ZoneId.of("America/Sao_Paulo");
        return LocalDateTime.now().atZone(zonaBrasil).toLocalDateTime();
    }

}
