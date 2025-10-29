package by.softclub.service;

import by.softclub.config.BotConfig;
import by.softclub.utils.TelegramBotUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Slf4j
@Component
public class SqlQueryNormalizerBot extends TelegramLongPollingBot {

    private static final int TELEGRAM_MESSAGE_LIMIT = 3500; //todo

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
        this.replyKeyboardMarkup = TelegramBotUtils.getReplyKeyboardMarkup();
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
            } else if (messageText.length() > TELEGRAM_MESSAGE_LIMIT) {
                sendMessage(chatId, "The request is too big. Try inserting it as a .txt or .sql file");
            } else if (sqlNormalizerService.isCorrectSQLQuery(messageText)) {
                final String normalizedQuery = sqlNormalizerService.normalizeQuery(messageText, languageCode);
                sendNormalizedSql(chatId, normalizedQuery, "normalized_query.sql");
            } else {
                sendMessage(chatId, "Unknown request. Not exist SELECT or BIND statement in the request.");
            }
        } else if (update.hasMessage() && update.getMessage().hasDocument()) {
            handleDocumentMessage(update.getMessage());
        }
    }

    private void handleDocumentMessage(Message message) {
        long chatId = message.getChatId();
        Document document = message.getDocument();

        String fileName = document.getFileName();
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();

        if (!List.of("sql", "txt").contains(extension)) {
            sendMessage(chatId, "Please send a file with .sql or .txt extension.");
            return;
        }

        if (this.languageCode == null) {
            sendMessage(chatId, "Please select SQL dialect first using /start.");
            return;
        }

        if (document.getFileSize() > 1_000_000) {
            sendMessage(chatId, "File too large. Max size: 1 MB.");
            return;
        }

        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(document.getFileId());
            org.telegram.telegrambots.meta.api.objects.File fileMeta = execute(getFile);
            String filePath = fileMeta.getFilePath();

            String fileUrl = "https://api.telegram.org/file/bot" + config.getToken() + "/" + filePath;
            byte[] fileBytes = TelegramBotUtils.downloadSQLFile(fileUrl);

            String originalSql = new String(fileBytes, StandardCharsets.UTF_8);

            String normalizedSql = sqlNormalizerService.normalizeQuery(originalSql, languageCode);

            sendNormalizedSql(chatId, normalizedSql, "normalized_" + fileName);

        } catch (Exception e) {
            log.error("Error processing SQL file", e);
            sendMessage(chatId, "Failed to process your SQL file. Please try again.");
        }
    }

    private void sendNormalizedSql(long chatId, String normalizedSql, String fileName) {
        if (normalizedSql == null) {
            sendMessage(chatId, "Normalization returned empty result.");
            return;
        }

        String formattedText = "```\n" + normalizedSql + "\n```";

        if (formattedText.length() <= TELEGRAM_MESSAGE_LIMIT) {
            sendMessage(chatId, formattedText, null, ParseMode.MARKDOWNV2);
        } else {
            TelegramBotUtils.sendSqlFile(chatId, normalizedSql, fileName, (sendDocument) -> {
                try {
                    execute(sendDocument);
                } catch (TelegramApiException e) {
                    log.error("Failed to send SQL as file", e);
                    sendMessage(chatId, "Result is too large and cannot be sent even as file.");
                }
            });
        }
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
}