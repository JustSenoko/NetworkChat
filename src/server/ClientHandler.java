package server;

import message.MessagePatterns;
import message.TextMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static message.MessagePatterns.*;

class ClientHandler {

    private final Socket socket;
    private final String login;
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

    void sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
        System.out.println(msg);
    }
}