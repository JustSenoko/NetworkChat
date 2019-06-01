package server;

import message.MessagePatterns;
import message.TextMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static message.MessagePatterns.*;

class ClientHandler {

    private final Socket socket;
    private String login;
    private final ChatServer chatServer;
    private final static Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private DataOutputStream out;

    ClientHandler(String login, Socket socket, ChatServer chatServer, FileHandler logFileHandler) throws IOException {

        this.login = login;
        this.socket = socket;
        this.chatServer = chatServer;
        LOGGER.addHandler(logFileHandler);

        start();
    }

    private void start() throws IOException {
        chatServer.getExecutorService().execute(this::receiveMessages);
        out = new DataOutputStream(socket.getOutputStream());
    }

    private void receiveMessages() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            boolean clientDisconnected = false;
            while (!clientDisconnected) {
                String msg = in.readUTF();
                String msgCommand = msg.split(" ")[0];
                switch (msgCommand) {
                    case MESSAGE_PREFIX:
                        TextMessage textMessage = MessagePatterns.parseSendMessage(msg);
                        if (textMessage != null) {
                            chatServer.sendTextMessage(textMessage);
                        }
                        break;
                    case DISCONNECTED:
                        chatServer.unsubscribe(login);
                        clientDisconnected = true;
                        break;
                    case USER_INFO_PREFIX:
                        User newUserInfo = MessagePatterns.parseUserInfoMessage(msg);
                        chatServer.updateUserInfo(login, newUserInfo);
                        break;
                    default:
                        LOGGER.warning(String.format(EX_MESSAGE_PATTERN, msg));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLogin(String login) {
        this.login = login;
    }

    void sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
        LOGGER.fine(msg);
    }
}