package client;

import server.authorization.users.User;
import message.TextMessage;

import java.util.Set;

public interface MessageReceiver {

    void submitMessage(TextMessage message);

    void updateUserList(Set<String> userList);

    void userConnected(String login);

    void userDisconnected(String login);

    void updateUserInfo(User user);

    void changeUserLogin(String[] oldNewLogin);
}
