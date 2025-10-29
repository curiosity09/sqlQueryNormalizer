package by.softclub.utils;

import by.softclub.service.MessageKey;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TelegramBotUtils {

    public TelegramBotUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] downloadSQLFile(String fileUrl) throws IOException {
        final URL url = new URL(fileUrl);
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendSqlFile(long chatId, String sqlContent, String fileName, Consumer<SendDocument> executeDoc) {
        ByteArrayInputStream bis = new ByteArrayInputStream(sqlContent.getBytes(StandardCharsets.UTF_8));
        InputFile inputFile = new InputFile(bis, fileName);

        SendDocument sendDoc = new SendDocument();
        sendDoc.setChatId(String.valueOf(chatId));
        sendDoc.setDocument(inputFile);
        sendDoc.setCaption("Here is your normalized SQL file!");

        executeDoc.accept(sendDoc);
    }

    public static ReplyKeyboardMarkup getReplyKeyboardMarkup() {
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