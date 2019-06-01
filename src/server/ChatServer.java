package server;

import server.authorization.AuthorizationService;
import server.authorization.AuthorizationServiceImpl;
import persistence.UserRepository;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static message.MessagePatterns.USERS_PATTERN;

class ChatServer {

    private static final int SERVER_PORT = 7777;
    private ExecutorService executorService;
    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private static final String LOG_FILE_NAME = "log_file.log";
    private static FileHandler logFileHandler;
    private final AuthorizationService authService;
    private final Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {

        AuthorizationService auth;
        try {
            logFileHandler = new FileHandler(LOG_FILE_NAME, true);
            logFileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(logFileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String url = "jdbc:mysql://localhost:3306/network_chat?" +
                    "useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false" +
                    "&serverTimezone=Europe/Moscow&useSSL=false&allowPublicKeyRetrieval=true";
            Connection connection = DriverManager.getConnection(url, "root", "senoko");
            UserRepository userRepository = new UserRepository(connection);
            auth = new AuthorizationServiceImpl(userRepository, logFileHandler);
        } catch (SQLException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Не удалось подключиться к базе SQL", e);
            return;
        }
        ChatServer chatServer = new ChatServer(auth);
        chatServer.start();
    }

    private ChatServer(AuthorizationService authService) {
        this.authService = authService;
    }

    private void start() {

        this.executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            LOGGER.info("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                LOGGER.info("New client connected!");
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
                    LOGGER.info(String.format("User %s authorized successful!%n", userDB.getLogin()));
                    out.writeUTF(MessagePatterns.authResult(true));
                    out.flush();
                    subscribe(userDB, socket);
                } else {
                    LOGGER.warning(String.format("Wrong server.authorization for user %s%n", user.getLogin()));
                    out.writeUTF(MessagePatterns.authResult(false));
                    out.flush();
                    socket.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, "Проблемы с подключением к сокету", ex);
        }
        executorService.shutdown();
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
            LOGGER.warning(msg);
            userTo = userFrom;
        }
        if (userTo == null || userTo.trim().isEmpty()) {
            return;
        }
        LOGGER.fine(String.format("%s -> %s: %s%n", userFrom, userTo, msg));
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
        clientHandlerMap.put(login, new ClientHandler(login, socket, this, logFileHandler));
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

    ExecutorService getExecutorService() {
        return executorService;
    }
}
