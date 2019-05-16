package persistence;

import client.LogException;
import message.MessagePatterns;
import message.TextMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    private final int logSize;
    private final File file;

    public MessageRepository(String fileName, int logSize) throws LogException {
        this.logSize = logSize;
        Path path = Paths.get(fileName);
        file = path.toFile();
        if (!Files.exists(path)) {
            System.out.println("Файл лога не найден");
            try {
                if (file.createNewFile()) {
                    System.out.println("Создан новый файл лога");
                } else {
                    throw new LogException("Ошибка создания файла лога.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new LogException("Ошибка создания файла лога.");
            }
        }
    }

    public void logMessage(TextMessage msg, String login) throws IOException {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
            writer.println(MessagePatterns.logMessage(msg, login));
        }
    }

    public List<TextMessage> loadMessages(String login) throws IOException {

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
                    TextMessage msg = MessagePatterns.parseLogMessage(builder.reverse().toString(), login);
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
}
