package client.history;

import message.TextMessage;

import java.io.IOException;
import java.util.List;

public interface ChatHistory {

    void addMessage(TextMessage msg, String login);

    List<TextMessage> loadChatHistory(String login) throws IOException;

    void flush();

    TextMessage parseHistoryMessage(String msg, String login);

    String historyMessage(TextMessage msg, String login);
}
