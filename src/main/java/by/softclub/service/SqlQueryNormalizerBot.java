package by.softclub.service;

import by.softclub.config.BotConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Component
public class SqlQueryNormalizerBot extends TelegramLongPollingBot {

    public static final int ORACLE_LANG_NUM = 1;

    private final BotConfig config;
    private String languageCode;
    private ReplyKeyboardMarkup replyKeyboardMarkup;

    @Autowired
    public SqlQueryNormalizerBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @PostConstruct
    public void init(){
        replyKeyboardMarkup = getReplyKeyboardMarkup();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().toUpperCase();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains(MessageKey.SELECT.code()) && messageText.contains(MessageKey.BIND.code())) {
                sendMessage(chatId, normalizeQuery(messageText));
            } else if (Arrays.asList(MessageKey.ORACLE.code(), MessageKey.POSTGRESQL.code()).contains(messageText)) {
                setLanguageCode(messageText);
                sendMessage(chatId, "Great, you pick: " + messageText + "! Now send your query.");
            } else if (messageText.contains("/start")) {
                sendMessage(chatId, "What sql language do you want to use?", replyKeyboardMarkup);
            } else {
                sendMessage(chatId, "Unknown request. Not exist SELECT or BIND statement in the request.");
            }
        }
    }

    private String normalizeQuery(final String initialQuery) {
        String query = initialQuery.substring(initialQuery.indexOf(MessageKey.SELECT.code()), initialQuery.indexOf(MessageKey.BIND.code())).trim();
        String bind = initialQuery.substring(initialQuery.indexOf(MessageKey.BIND.code()) + MessageKey.BIND.code().length(), initialQuery.lastIndexOf(MessageKey.END_BRACKET.code())).trim();
        return replaceParams(bind, query, defineLanguage(getLanguageCode()));
    }

    public String replaceParams(final String bind, String query, final int languageNumber) {
        String[] split = bind.split(", ");
        for (String param : split) {
            if (param.matches("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}).\\d+$")) {
                param = String.format("TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS.FF')", param);
            } else if (!param.matches("\\d+") && !param.matches("^(true|false|TRUE|FALSE)$")) {
                param = String.format("'%s'", param);
            } else if (param.matches("^(true|false|TRUE|FALSE)$") && languageNumber == ORACLE_LANG_NUM) {
                param = String.format("%s", param.matches("^(true|TRUE)$") ? 1 : 0);
            }
            query = query.replaceFirst("[?]", param);
        }
        return query;
    }

    private void sendMessage(final Long chatId, final String textToSend, final ReplyKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
//        sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        if (Objects.nonNull(markup)) {
            sendMessage.setReplyMarkup(markup);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    private void sendMessage(final Long chatId, final String textToSend) {
        sendMessage(chatId, textToSend, null);
    }

    private int defineLanguage(final String languageCode) {
        final MessageKey messageKey = Optional.ofNullable(languageCode)
                .flatMap(MessageKey::parse)
                .orElse(MessageKey.UNKNOWN);

        return switch (messageKey) {
            case ORACLE -> 1;
            case POSTGRESQL -> 0;
            default -> -1;
        };
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setInputFieldPlaceholder("Write your query...");
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(MessageKey.ORACLE.code());
        keyboardButtons.add(MessageKey.POSTGRESQL.code());
        keyboardRows.add(keyboardButtons);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}