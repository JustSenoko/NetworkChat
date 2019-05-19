package client.swing;

import client.UnreadMessage;

import javax.swing.*;
import java.awt.*;

class UserListCellRenderer extends JPanel implements ListCellRenderer<UnreadMessage> {

    private final JLabel lbUserLogin;
    private final JLabel lbUnread;

    UserListCellRenderer() {

        setLayout(new BorderLayout());

        lbUserLogin = new JLabel();
        Font f = new Font("Verdana", Font.BOLD, 14);
        lbUserLogin.setFont(f);
        add(lbUserLogin, BorderLayout.CENTER);

        lbUnread = new JLabel();
        lbUnread.setFont(f.deriveFont(f.getStyle() | Font.PLAIN));
        lbUnread.setBackground(Color.CYAN);

        add(lbUnread, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends UnreadMessage> list,
                                                  UnreadMessage value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        setBackground(list.getBackground());
        lbUserLogin.setText(value.getLogin());
        lbUnread.setText(String.format("%d", value.getUnreadCount()));
        lbUnread.setVisible(value.getUnreadCount() != 0);
        if (isSelected) {
            setBackground(Color.GRAY);
        }

        return this;
    }
}
