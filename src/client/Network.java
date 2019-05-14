package client;

import server.authorization.AuthException;
import server.authorization.users.User;
import message.TextMessage;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import static message.MessagePatterns.*;

public class Network implements Closeable {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String hostName;
    private int port;
    private MessageReceiver messageReceiver;
    private UserInfoReceiver userInfoReceiver;

    private String login;
    private User user;

    private Thread receiverThread;

    public Network(String hostName, int port, MessageReceiver messageReceiver) {
        this.hostName = hostName;
        this.port = port;
        this.messageReceiver = messageReceiver;

        this.receiverThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String text = in.readUTF();
                    System.out.printf("New message > %s%n", text);
                    parseMessage(text);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (socket.isClosed()) {
                        break;
                    }
                }
            }
        });
    }

    public void authorize(String login, String password) throws IOException, AuthException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(AUTH_SEND_PATTERN, login, password));
        String response = in.readUTF();
        if (response.equals(authResult(true))) {
            this.login = login;
            receiverThread.start();
        } else {
            throw new AuthException("Неверный логин/пароль");
        }
    }

    public void addUser(String login, String password, String name) throws IOException, AuthException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(REG_SEND_PATTERN, login, password, name));
        String response = in.readUTF();
        if (!response.equals(regResult(true))) {
            throw new AuthException("Пользователь с таким логином уже зарегистрирован");
        }
    }

    public void updateUserInfo(UserInfoReceiver resultReceiver, String newLogin, String newPassword, String newName) {
        sendMessage(String.format(USER_INFO_RESULT_PATTERN, newLogin, newPassword, newName));
        userInfoReceiver = resultReceiver;
    }

    public void sendTextMessage(TextMessage message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserTo(), login, message.getMessage()));
    }

    private void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }

    @Override
    public void close() {
        this.receiverThread.interrupt();
        sendMessage(DISCONNECTED);
    }

    private void parseMessage(String msg) {
        String msgCommand = msg.split(" ")[0];
        String userLogin;

        switch (msgCommand) {
            case MESSAGE_PREFIX:
                TextMessage textMessage = parseSendMessage(msg);
                if (textMessage != null) {
                    messageReceiver.submitMessage(textMessage);
                }
                break;
            case CONNECTED:
                userLogin = parseConnectMessage(msg);
                if (userLogin != null) {
                    messageReceiver.userConnected(userLogin);
                }
                break;
            case DISCONNECTED:
                userLogin = parseDisconnectMessage(msg);
                if (userLogin != null) {
                    messageReceiver.userDisconnected(userLogin);
                }
                break;
            case USERS_PREFIX:
                Set<String> userList = parseUserListMessage(msg);
                messageReceiver.updateUserList(userList);
                break;
            case USER_INFO_PREFIX:
                user = parseUserInfoMessage(msg);
                if (user != null) {
                    login = user.getLogin();
                    messageReceiver.updateUserInfo(user);
                }
                break;
            case UPD_USER_INFO_PREFIX:
                boolean success = msg.equals(updUserInfoResult(true));
                userInfoReceiver.interpretUpdateUserInfoResult(success);
                break;
            case USER_LOGIN_CHANGED_PREFIX:
                String[] oldNewLogin = parseLoginChangeMessage(msg);
                messageReceiver.changeUserLogin(oldNewLogin);
                break;
            default:
                System.out.printf(EX_MESSAGE_PATTERN, msg);
        }
    }

    public User getUserInfo() {
        return user;
    }
}
