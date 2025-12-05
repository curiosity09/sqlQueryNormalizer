package by.softclub.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
public class SqlNormalizerService {
    private static final int ORACLE_LANG_NUM = 1;

    public String normalizeQuery(final String initialQuery, final String langCode) {
        final String query = initialQuery.substring(
                StringUtils.indexOfIgnoreCase(initialQuery, MessageKey.SELECT.code()), StringUtils.indexOfIgnoreCase(initialQuery, MessageKey.BIND.code())
        ).trim();
        final String bind = initialQuery.substring(
                StringUtils.indexOfIgnoreCase(initialQuery, MessageKey.BIND.code()) + MessageKey.BIND.code().length(), StringUtils.lastIndexOfIgnoreCase(initialQuery, MessageKey.END_BRACKET.code())
        ).trim();
        return replaceParams(bind, query, defineLanguage(langCode));
    }

    public boolean isCorrectSQLQuery(String messageText) {
        return StringUtils.containsIgnoreCase(messageText, MessageKey.SELECT.code())
                && StringUtils.containsIgnoreCase(messageText, MessageKey.BIND.code());
    }

    private String replaceParams(final String bind, String query, final int languageNumber) {
        String[] split = bind.split(", ");
        for (String param : split) {
            if (param.matches("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}).\\d+$")) {
                param = String.format("TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS.FF')", param);
            } else if (param.matches("^(\\d{4})-(\\d{2})-(\\d{2})$")) {
                param = String.format("TO_TIMESTAMP('%s', 'YYYY-MM-DD')", param);
            } else if (!param.matches("\\d+") && !param.matches("^(true|false|TRUE|FALSE)$")) {
                param = String.format("'%s'", param);
            } else if (param.matches("^(true|false|TRUE|FALSE)$") && languageNumber == ORACLE_LANG_NUM) {
                param = String.format("%s", param.matches("^(true|TRUE)$") ? 1 : 0);
            }
            query = query.replaceFirst("[?]", param);
        }
        return query;
    }

    private int defineLanguage(final String languageCode) {
        final MessageKey messageKey = Optional.ofNullable(languageCode)
                .flatMap(MessageKey::parse)
                .orElse(MessageKey.UNKNOWN);

        return switch (messageKey) {
            case ORACLE, UNKNOWN -> 1;
            case POSTGRESQL -> 0;
            default -> -1;
        };
    }
}
