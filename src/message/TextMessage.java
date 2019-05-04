package message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TextMessage {
    private String userFrom;
    private String userTo;
    private String message;
    private LocalDateTime date;

    public TextMessage(String userTo, String userFrom, String message) {
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.message = message;
        this.date = LocalDateTime.now();
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

    public String getDateFormatted() {
        String formatPattern = "HH:mm";
        return date.format(DateTimeFormatter.ofPattern(formatPattern));
    }
}
