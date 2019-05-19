package message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TextMessage {
    private final String userFrom;
    private final String userTo;
    private final String message;
    private final LocalDateTime date;

    public TextMessage(String userTo, String userFrom, String message) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.message = message;
        this.date = LocalDateTime.now();
    }

    public TextMessage(String userTo, String userFrom, String message, LocalDateTime date) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.message = message;
        this.date = date;
    }

    public String getUserFrom() {
        return userFrom;
    }

    public String getUserTo() {
        return userTo;
    }

    public String getMessage() {
        return message;
    }

    public String getDateFormatted(String formatPattern) {
        return date.format(DateTimeFormatter.ofPattern(formatPattern));
    }
}
