package server;

import authorization.users.User;
import authorization.AuthorizationService;
import authorization.AuthorizationServiceImpl;
import authorization.users.UserRepository;
import message.MessagePatterns;
import message.TextMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static message.MessagePatterns.USERS_PATTERN;

public class ChatServer {

    private static final int SERVER_PORT = 7777;
    private AuthorizationService authService;
    private Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {

        AuthorizationService auth;
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/network_chat",
                    "root", "senoko");
            UserRepository userRepository = new UserRepository(connection);
            auth = new AuthorizationServiceImpl(userRepository);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        ChatServer chatServer = new ChatServer(auth);
        chatServer.start();
    }

    private ChatServer(AuthorizationService authService) {
        this.authService = authService;
    }


    private void start() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("New client connected!");
                User user;
                try {
                    String inpMessage = inp.readUTF();
                    user = authService.checkAuthorization(inpMessage);
                    if (user == null && registerNewUser(inpMessage)) {
                        out.writeUTF(MessagePatterns.regResult(true));
                        out.flush();
                        socket.close();
                        continue;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    continue;
                }

                if (user == null) {
                    out.writeUTF(MessagePatterns.authResult(false));
                    out.flush();
                    socket.close();
                    continue;
                }

                User userDB = authService.authUser(user);
                if (userDB != null && !userIsOnline(userDB.getLogin())) {
                    System.out.printf("User %s authorized successful!%n", userDB.getLogin());
                    out.writeUTF(MessagePatterns.authResult(true));
                    out.flush();
                    subscribe(userDB, socket);
                } else {
                    System.out.printf("Wrong authorization for user %s%n", user.getLogin());
                    out.writeUTF(MessagePatterns.authResult(false));
                    out.flush();
                    socket.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean registerNewUser(String regMessage) {
        User user = authService.checkRegistration(regMessage);
        if (user != null) {
            return authService.addUser(user);
        }
        return false;
    }

    private void sendMessageFromUserToUser(String userTo, String userFrom, String msg) {

        if (!userIsOnline(userTo)) {
            msg = String.format("Не удалось отправить сообщение. Пользователь %s не в сети.%n", userTo);
            userTo = userFrom;
        }
        if (userTo == null || userTo.trim().isEmpty()) {
            return;
        }
        String msgText = String.format(MessagePatterns.MESSAGE_SEND_PATTERN, userTo, userFrom, msg);
        sendMessage(userTo, msgText);
    }

    private void sendMessage(String userTo, String msg) {
        ClientHandler userToClientHandler = clientHandlerMap.get(userTo);
        try {
            userToClientHandler.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean userIsOnline(String login) {
        return clientHandlerMap.containsKey(login);
    }

    private void sendUserListMessage(String login) {
        Set<String> users = clientHandlerMap.keySet();
        String userList = String.format(USERS_PATTERN, users.toString());

        sendMessage(login, userList);
    }

    private void sendUserInfoMessage(User user) {
        String userInfo = String.format(MessagePatterns.USER_INFO_RESULT_PATTERN, user.getLogin(), user.getPassword(), user.getName());
        sendMessage(user.getLogin(), userInfo);
    }


    void sendTextMessage(TextMessage textMessage) {
        sendMessageFromUserToUser(textMessage.getUserTo(), textMessage.getUserFrom(), textMessage.getMessage());
    }

    private void subscribe(User user, Socket socket) throws IOException {
        String login = user.getLogin();
        clientHandlerMap.put(login, new ClientHandler(login, socket, this));
        String msg = String.format(MessagePatterns.CONNECTED_SEND, login);
        sendToAllUsersExceptLogin(login, msg);

        // отправим сразу всю информацию о пользователе
        sendUserInfoMessage(user);
        sendUserListMessage(login);
    }

    void unsubscribe(String login) {
        clientHandlerMap.remove(login);

        String msg = String.format(MessagePatterns.DISCONNECTED_SEND, login);
        sendToAllUsersExceptLogin(login, msg);
    }

    private void sendToAllUsersExceptLogin(String login, String msg) {
        Set<String> users = clientHandlerMap.keySet();
        for (String userLogin : users) {
            if (userLogin.equals(login)) {
                continue;
            }
            sendMessage(userLogin, msg);
        }
    }

    void updateUserInfo(String oldLogin, User newUserInfo) {
        boolean success = authService.updateUserInfo(oldLogin, newUserInfo);
        sendMessage(oldLogin, MessagePatterns.updUserInfoResult(success));
        if (success) {
            String newLogin = newUserInfo.getLogin();
            if (newLogin.equals(oldLogin)) {
                sendUserInfoMessage(newUserInfo);
                return;
            }
            String msg = String.format(MessagePatterns.USER_LOGIN_CHANGED_SEND_PATTERN, oldLogin, newLogin);
            sendToAllUsersExceptLogin(oldLogin, msg);

            ClientHandler clH = clientHandlerMap.get(oldLogin);
            clH.setLogin(newLogin);
            clientHandlerMap.put(newLogin, clH);
            clientHandlerMap.remove(oldLogin);
            sendUserInfoMessage(newUserInfo);
        }
    }
}
