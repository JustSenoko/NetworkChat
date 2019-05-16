package client.swing;

import client.LogException;
import client.MessageReceiver;
import client.Network;
import client.UnreadMessage;
import message.TextMessage;
import persistence.MessageRepository;
import server.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainWindow extends JFrame implements MessageReceiver {

    private static final String SERVER_ADDR = "localhost";
    private static final int SERVER_PORT = 7777;

    private final JList<TextMessage> msgList;
    private final Map<String, DefaultListModel<TextMessage>> userMsgModelMap = new HashMap<>();
    private final Map<String, UnreadMessage> userUnreadMsgMap = new HashMap<>();

    private final JTextField messageField;
    private final JList<UnreadMessage> userList;
    private final DefaultListModel<UnreadMessage> userListModel;
    private final JButton btnUserInfo;

    private Network network;
    private String userTo = "";
    private MessageRepository msgRep;

    public MainWindow(int logSize, String logFileNameTempl) {

        setTitle("Чат");
        setBounds(200,200, 500, 500);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (network != null) {
                    network.close();
                }
                super.windowClosing(e);
            }
        });

        setLayout(new BorderLayout());

        JPanel userPanel = new JPanel();
        userPanel.setSize(new Dimension(100, 0));
        userPanel.setLayout(new BorderLayout());

        btnUserInfo = new JButton();
        btnUserInfo.setBackground(Color.BLUE);
        btnUserInfo.setForeground(Color.WHITE);
        btnUserInfo.setHorizontalAlignment(SwingConstants.CENTER);
        btnUserInfo.addActionListener(e -> {
            UserInfoDialog userInfoDialog = new UserInfoDialog(this, network);
            userInfoDialog.setVisible(true);
        });
        userPanel.add(btnUserInfo, BorderLayout.NORTH);

        userList = new JList<>();
        userListModel = new DefaultListModel<>();
        userList.setModel(userListModel);
        UserListCellRenderer userListCellRenderer = new UserListCellRenderer();
        userList.setCellRenderer(userListCellRenderer);
        userList.addListSelectionListener(e -> setUserTo());

        JScrollPane scrollUsers = new JScrollPane(userList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        userPanel.add(scrollUsers, BorderLayout.CENTER);

        add(userPanel, BorderLayout.WEST);

        msgList = new JList<>();

        JScrollPane scrollMessages = new JScrollPane(msgList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollMessages, BorderLayout.CENTER);

        JPanel sendMessagePanel = new JPanel();
        sendMessagePanel.setLayout(new BorderLayout());

        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        sendMessagePanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Отправить");
        sendButton.isDefaultButton();
        sendButton.addActionListener(e -> sendMessage());
        sendMessagePanel.add(sendButton, BorderLayout.EAST);

        add(sendMessagePanel, BorderLayout.SOUTH);

        setVisible(true);

        this.network = new Network(SERVER_ADDR, SERVER_PORT, this);

        LoginDialog loginDialog = new LoginDialog(this, network);
        loginDialog.setVisible(true);

        if (!loginDialog.isConnected()) {
            System.exit(0);
        }

        try {
            String logFileName = String.format(logFileNameTempl, network.getLogin());
            msgRep = new MessageRepository(logFileName, logSize);
            loadChatLog(msgRep.loadMessages(network.getLogin()));
        } catch (LogException e) {
            e.printStackTrace();
            showErrorMessage(e.getMessage(), "Лог чата");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Не удалось загрузить историю чата", "Лог чата");
        }

        setChatTitle(network.getLogin());
        TextMessageCellRenderer msgListCellRenderer = new TextMessageCellRenderer(network.getLogin());
        msgList.setCellRenderer(msgListCellRenderer);
    }

    private void setChatTitle(String login) {
        setTitle(String.format("Чат (%s)", login));
    }

    private void setUserTo() {

        if (userList.getSelectedValue() == null) {
            return;
        }

        userTo = userList.getSelectedValue().getLogin();
        if (userTo == null && userListModel.size() > 0) {
            userTo = userListModel.elementAt(0).getLogin();
        }
        if (userTo != null) {
            if (userMsgModelMap.containsKey(userTo)) {
                msgList.setModel(userMsgModelMap.get(userTo));
            }
            if (userUnreadMsgMap.containsKey(userTo)) {
                userUnreadMsgMap.get(userTo).resetUnreadCount();
            }
        }
    }

    private void sendMessage() {
        String msg = messageField.getText();
        if (msg == null || msg.trim().isEmpty() || userTo.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(userTo, network.getLogin(), msg);
        DefaultListModel<TextMessage> msgListModel = userMsgModelMap.get(userTo);
        msgListModel.add(msgListModel.size(), textMessage);
        messageField.setText("");
        network.sendTextMessage(textMessage);

        logMessage(textMessage);
    }

    @Override
    public void submitMessage(TextMessage message) {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<TextMessage> msgListModel = userMsgModelMap.get(message.getUserFrom());
            msgListModel.add(msgListModel.size(), message);
            if (userTo.equals(message.getUserFrom())) {
                msgList.ensureIndexIsVisible(msgListModel.size() - 1);
            } else {
                userUnreadMsgMap.get(message.getUserFrom()).incUnreadCount();
                userList.updateUI();
            }
            logMessage(message);
        });
    }

    @Override
    public void updateUserList(Set<String> userSet) {

        String login = network.getLogin();
        SwingUtilities.invokeLater(() -> {
            userListModel.removeAllElements();
            for (String userLogin : userSet) {
                if (userLogin.equals(login)) {
                    continue;
                }
                addUser(userLogin);
            }
            updateUserListView();
        });
    }

    private void addUser(String login) {
        UnreadMessage newUser = new UnreadMessage(login);
        userListModel.addElement(newUser);
        userMsgModelMap.putIfAbsent(login, new DefaultListModel<>());
        userUnreadMsgMap.putIfAbsent(login, newUser);
    }

    private void updateUserListView() {
        if (userListModel.size() == 1
                || (userListModel.size() > 1 && userList.getSelectedValue() == null)) {
            userList.setSelectedIndex(0);
        }
        setUserTo();
    }

    @Override
    public void userConnected(String login) {
        SwingUtilities.invokeLater(() -> {
            int ix = userListModel.indexOf(userUnreadMsgMap.get(login));
            if (ix == -1) {
                addUser(login);
            }
            updateUserListView();
        });
    }

    @Override
    public void userDisconnected(String login) {
        SwingUtilities.invokeLater(() -> {
            int ix = userListModel.indexOf(userUnreadMsgMap.get(login));
            if (ix >= 0) {
                userListModel.remove(ix);
                //из модели ссобщений не удаляем - если он переподключится, сохранится история
            }
            updateUserListView();
        });
    }

    @Override
    public void updateUserInfo(User user) {
        String userInfoView = String.format("%s (%s)", user.getName(), user.getLogin());
        btnUserInfo.setText(userInfoView);
        setChatTitle(user.getLogin());
    }

    @Override
    public void changeUserLogin(String[] oldNewLogin) {
        SwingUtilities.invokeLater(() -> {
            String oldLogin = oldNewLogin[0];
            String newLogin = oldNewLogin[1];
            int ix = userListModel.indexOf(userUnreadMsgMap.get(oldLogin));
            if (ix >= 0) {
                int unreadCount = userUnreadMsgMap.get(oldLogin).getUnreadCount();
                UnreadMessage newUser = new UnreadMessage(newLogin, unreadCount);
                userUnreadMsgMap.remove(oldLogin);
                userUnreadMsgMap.put(newLogin, newUser);

                userListModel.remove(ix);
                userListModel.add(ix, newUser);

                DefaultListModel<TextMessage> msgModel = userMsgModelMap.get(oldLogin);
                userMsgModelMap.put(newLogin, msgModel);
                userMsgModelMap.remove(oldLogin);

                userList.updateUI();
            }
            updateUserListView();

            //@TODO обновить этот логин в истории
        });
    }

    @Override
    public void showErrorMessage(String msg, String title) {
        JOptionPane.showMessageDialog(MainWindow.this,
                msg, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void loadChatLog(List<TextMessage> chatLog) {
        String currentLogin = network.getLogin();
        for (TextMessage msg : chatLog) {
            String login = (msg.getUserFrom().equals(currentLogin)) ? (msg.getUserTo()) : (msg.getUserFrom());
            userMsgModelMap.putIfAbsent(login, new DefaultListModel<>());

            DefaultListModel<TextMessage> msgListModel = userMsgModelMap.get(login);
            msgListModel.addElement(msg);
        }
    }

    @Override
    public void logMessage(TextMessage msg) {
        try {
            msgRep.logMessage(msg, network.getLogin());
        } catch (IOException e) {
            showErrorMessage("Не удалось сохранить историю сообщений", "Ошибка истории сообщений");
            e.printStackTrace();
        }
    }
}
