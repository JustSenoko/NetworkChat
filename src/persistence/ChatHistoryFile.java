package persistence;

import client.history.ChatHistoryException;
import client.history.ChatHistory;
import message.TextMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHistoryFile implements ChatHistory {

    private final int logSize;
    private final File file;
    private final PrintWriter historyWriter;

    private static final String MSG_LOG_PATTERN = "%s %s %s %s"; //login (1 = in/0 = out) date text
    private static final Pattern MSG_LOG_REC_PATTERN = Pattern.compile(
            "^(\\w+) ([01]) (\\d{4}-[01]\\d-[0-3]\\d [012]\\d:[0-5]\\d) (.+)", Pattern.MULTILINE);
    public static final String MSG_DATE_PATTERN = "yyyy-MM-dd HH:mm"; //2019-05-15 14:00

    public ChatHistoryFile(String fileName, int logSize) throws ChatHistoryException {
        this.logSize = logSize;
        Path path = Paths.get(fileName);
        file = path.toFile();
        if (!Files.exists(path)) {
            System.out.println("Файл лога не найден");
            try {
                if (file.createNewFile()) {
                    System.out.println("Создан новый файл лога");
                } else {
                    throw new ChatHistoryException("Ошибка создания файла лога.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new ChatHistoryException("Ошибка создания файла лога.");
            }
        }
        try {
            historyWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file, true)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ChatHistoryException("Не найден файл для сохранения истории.");
        }
    }

    @Override
    public synchronized void addMessage(TextMessage msg, String login) {
        historyWriter.println(historyMessage(msg, login));
    }

    @Override
    public List<TextMessage> loadChatHistory(String login) throws IOException {

        List<TextMessage> result = new ArrayList<>();

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {

            int lines = 0;
            StringBuilder builder = new StringBuilder();
            long length = file.length();
            if (length == 0) {
                return result;
            }
            length--;
            randomAccessFile.seek(length);
            for (long seek = length; seek >= 0 && lines < logSize; --seek) {
                randomAccessFile.seek(seek);
                char c = (char) randomAccessFile.read();
                if (c == '\r') {
                    continue;
                } else if (c != '\n') {
                    builder.append(c);
                }
                if (c == '\n' || seek == 0) {
                    TextMessage msg = parseHistoryMessage(builder.reverse().toString(), login);
                    if (msg != null) {
                        result.add(0, msg);
                        lines++;
                    }
                    builder = new StringBuilder();
                }
            }
        }
        return result;
    }

    @Override
    public void flush() {
        historyWriter.flush();
    }

     public static TextMessage parseHistoryMessage(String msg, String login) {
        Matcher matcher = MSG_LOG_REC_PATTERN.matcher(msg);
        if (matcher.matches()) {
            String userTo = login;
            String userFrom = login;
            LocalDateTime date = parseDate(matcher.group(3));
            if (matcher.group(2).equals("1")) {
                userFrom = matcher.group(1);
            } else {
                userTo = matcher.group(1);
            }
            return new TextMessage(userTo, userFrom, matcher.group(4), date);
        } else {
            System.out.printf("Unknown message pattern: %s%n", msg);
            return null;
        }
    }

    public static LocalDateTime parseDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(MSG_DATE_PATTERN);
        return LocalDateTime.parse(dateString, formatter);
    }

    public static String historyMessage(TextMessage msg, String login) {
        String loginInLog;
        int direction = 0; //0 - out, 1 - in

        if (msg.getUserTo().equals(login)) {
            loginInLog = msg.getUserFrom();
            direction = 1;
        } else {
            loginInLog = msg.getUserTo();
        }
        return String.format(MSG_LOG_PATTERN, loginInLog, direction,
                msg.getDateFormatted(MSG_DATE_PATTERN),
                msg.getMessage());
    }
}
