package server;

import authorization.users.User;
import message.MessagePatterns;
import message.TextMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static message.MessagePatterns.*;

class ClientHandler {

    private final Socket socket;
    private String login;
    private final ChatServer chatServer;
    private DataOutputStream out;

    ClientHandler(String login, Socket socket, ChatServer chatServer) throws IOException {

        this.login = login;
        this.socket = socket;
        this.chatServer = chatServer;

        start();
    }

    private void start() throws IOException {

        Thread receiveThread = new Thread(() -> {
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
                            System.out.printf(EX_MESSAGE_PATTERN, msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        out = new DataOutputStream(socket.getOutputStream());
        receiveThread.start();
    }

    public void setLogin(String login) {
        this.login = login;
    }

    void sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
        System.out.println(msg);
    }
}