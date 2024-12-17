package by.softclub.service;

import java.util.Objects;
import java.util.Optional;

public enum MessageKey {
    ORACLE("ORACLE"),
    POSTGRESQL("POSTGRESQL"),
    BIND("BIND => ["),
    SELECT("SELECT"),
    END_BRACKET("]"),
    UNKNOWN("unknown");
    final String code;

    MessageKey(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<MessageKey> parse(final String code) {
        for (MessageKey messageKey : MessageKey.values()) {
            if (Objects.equals(messageKey.code(), code)) {
                return Optional.of(messageKey);
            }
        }
        return Optional.empty();
    }
}
