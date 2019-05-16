package client;

import server.User;
import message.TextMessage;

import java.util.List;
import java.util.Set;

public interface MessageReceiver {

    void submitMessage(TextMessage message);

    void updateUserList(Set<String> userList);

    void userConnected(String login);

    void userDisconnected(String login);

    void updateUserInfo(User user);

    void changeUserLogin(String[] oldNewLogin);

    void showErrorMessage(String msg, String title);

    void loadChatLog(List<TextMessage> chatLog);

    void logMessage(TextMessage msg);
}
