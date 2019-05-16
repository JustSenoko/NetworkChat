package client;

import client.swing.MainWindow;

import javax.swing.*;

class ChatClient {

    private static MainWindow mainWindow;
    private static final int logSize = 2;
    private static final String logFileNameTempl = "chatLog_%s.txt"; // chatlog_login.txt

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> mainWindow = new MainWindow(logSize, logFileNameTempl));
    }
}
