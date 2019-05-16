package client.swing;

import message.TextMessage;

import javax.swing.*;
import java.awt.*;

class TextMessageCellRenderer extends JPanel implements ListCellRenderer<TextMessage> {

    private final JLabel msgUser;
    private final JTextArea msgText;
    private final JLabel msgData;
    private final String login; //текущий пользователь

    TextMessageCellRenderer(String login) {

        this.login = login;

        setLayout(new BorderLayout());
        setEnabled(false);

        msgUser = new JLabel();
        Font f = msgUser.getFont();
        msgUser.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

        add(msgUser, BorderLayout.CENTER);

        msgData = new JLabel();

        add(msgData, BorderLayout.EAST);

        msgText = new JTextArea();
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setPreferredSize(new Dimension(0, 15));

        add(msgText, BorderLayout.SOUTH);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TextMessage> list,
                                                  TextMessage textMessage, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        setBackground(list.getBackground());
        msgData.setText(textMessage.getDateFormatted("HH:mm"));
        msgText.setText(textMessage.getMessage());
        msgUser.setOpaque(true);

        String userFrom = textMessage.getUserFrom();
        if (isOutcomingMessage(userFrom)) {
            msgUser.setText("Вы");
        } else {
            msgUser.setText(userFrom);
            msgUser.setHorizontalAlignment(SwingConstants.LEFT);
//            msgUser.setBackground(Color.LIGHT_GRAY);
//            msgText.setBackground(Color.LIGHT_GRAY);
        }
        return this;
    }

    private boolean isOutcomingMessage(String userFrom) {
        return userFrom.equals(login);
    }
}
