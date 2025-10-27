package by.softclub.service;

import by.softclub.config.BotConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
@Getter
@Setter
@Slf4j
@Component
public class SqlQueryNormalizerBot extends TelegramLongPollingBot {

    private BotConfig config;
    private String languageCode;
    private ReplyKeyboardMarkup replyKeyboardMarkup;
    private SqlNormalizerService sqlNormalizerService;

    @Autowired
    public SqlQueryNormalizerBot(BotConfig config, SqlNormalizerService sqlNormalizerService) {
        super(config.getToken());
        this.config = config;
        this.sqlNormalizerService = sqlNormalizerService;
    }

    @PostConstruct
    public void init() {
        this.replyKeyboardMarkup = getReplyKeyboardMarkup();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/start")) {
                sendMessage(chatId, "What sql language do you want to use?", replyKeyboardMarkup, null);
            } else if (Arrays.asList(MessageKey.ORACLE.code(), MessageKey.POSTGRESQL.code()).contains(messageText)) {
                setLanguageCode(messageText);
                sendMessage(chatId, "Great, you pick: " + messageText + "! Now send your query.");
            } else if (isCorrectSQLQuery(messageText)) {
                sendMessage(chatId, "```\n" + sqlNormalizerService.normalizeQuery(messageText, languageCode) + "```", null, ParseMode.MARKDOWNV2);
            } else {
                sendMessage(chatId, "Unknown request. Not exist SELECT or BIND statement in the request.");
            }
        }
    }

    private boolean isCorrectSQLQuery(String messageText) {
        messageText = messageText.toUpperCase();
        return messageText.contains(MessageKey.SELECT.code()) && messageText.contains(MessageKey.BIND.code());
    }

    private void sendMessage(final Long chatId, final String textToSend, final ReplyKeyboardMarkup markup, final String parseMode) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setParseMode(ParseMode.HTML);
        if (Objects.nonNull(parseMode)) {
            sendMessage.setParseMode(parseMode);
        }
        if (Objects.nonNull(markup)) {
            sendMessage.setReplyMarkup(markup);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendMessage(final Long chatId, final String textToSend) {
        sendMessage(chatId, textToSend, null, null);
    }

    private ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setInputFieldPlaceholder("Write your query...");

        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(MessageKey.ORACLE.code());
        keyboardButtons.add(MessageKey.POSTGRESQL.code());

        replyKeyboardMarkup.setKeyboard(List.of(keyboardButtons));

        return replyKeyboardMarkup;
    }
}