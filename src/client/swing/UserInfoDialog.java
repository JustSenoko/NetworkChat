package client.swing;

import authorization.users.User;
import client.Network;
import client.UserInfoReceiver;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

class UserInfoDialog extends JDialog implements UserInfoReceiver {

    private final Network network;
    private User oldUserInfo;

    private final JPanel panel;
    private final GridBagConstraints cs;
    private final JTextField tfUsername;
    private final JTextField tfLogin;
    private final JPasswordField pfPassword;
    private final JPasswordField pfPasswordRepeat;
    private final JLabel lbUsername;
    private final JLabel lbLogin;
    private final JLabel lbPassword;
    private final JLabel lbPasswordRepeat;
    private final JButton btnSaveChanges;
    private final JButton btnCancel;

    UserInfoDialog(Frame parent, Network network) {
        super(parent, "Логин", true);
        this.network = network;

        panel = new JPanel(new GridBagLayout());
        cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        lbUsername = new JLabel("Имя пользователя: ");
        addComponent(lbUsername, 0, row, 1);

        tfUsername = new JTextField(20);
        addComponent(tfUsername, 1, row, 2);

        row++;

        lbLogin = new JLabel("Логин: ");
        addComponent(lbLogin, 0, row, 1);

        tfLogin = new JTextField(20);
        addComponent(tfLogin, 1, row, 2);

        row++;

        lbPassword = new JLabel("Пароль: ");
        addComponent(lbPassword, 0, row, 1);

        pfPassword = new JPasswordField(20);
        addComponent(pfPassword, 1, row, 2);

        row++;

        lbPasswordRepeat = new JLabel("Повторите пароль: ");
        addComponent(lbPasswordRepeat, 0, row, 1);

        pfPasswordRepeat = new JPasswordField(20);
        addComponent(pfPasswordRepeat, 1, row, 2);

        panel.setBorder(new LineBorder(Color.GRAY));

        btnSaveChanges = new JButton("Сохранить");
        btnSaveChanges.addActionListener(e -> saveChanges());

        btnCancel = new JButton("Отмена");
        btnCancel.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.add(btnSaveChanges);
        bp.add(btnCancel);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        fillInUserInfo();
    }

    private void fillInUserInfo() {
        oldUserInfo = network.getUserInfo();
        tfUsername.setText(oldUserInfo.getName());
        tfLogin.setText(oldUserInfo.getLogin());
        String psw = oldUserInfo.getPassword();
        pfPassword.setText(psw);
        pfPasswordRepeat.setText(psw);
    }

    private void saveChanges() {
        String newUserName = tfUsername.getText();
        String newLogin = tfLogin.getText();
        String newPassword = String.valueOf(pfPassword.getPassword());
        if (newLogin.isEmpty()) {
            JOptionPane.showMessageDialog(UserInfoDialog.this,
                    "Логин не может быть пустым",
                    "Сохранение данных",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newUserName.isEmpty()) {
            JOptionPane.showMessageDialog(UserInfoDialog.this,
                    "Имя пользователя не может быть пустым",
                    "Сохранение данных",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPassword.equals(String.valueOf(pfPasswordRepeat.getPassword()))) {
            JOptionPane.showMessageDialog(UserInfoDialog.this,
                    "Введенные пароли не совпадают",
                    "Сохранение данных",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newUserName.equals(oldUserInfo.getName())
                || !newLogin.equals(oldUserInfo.getLogin())
                || !newPassword.equals(oldUserInfo.getPassword())) {
                network.updateUserInfo(this, newLogin, newPassword, newUserName);
                return;
        }
        dispose();
    }

    private void addComponent(JComponent component, int gridx, int gridy, int gridwidth) {
        cs.gridx = gridx;
        cs.gridy = gridy;
        cs.gridwidth = gridwidth;
        panel.add(component, cs);
    }

    @Override
    public void interpretUpdateUserInfoResult(boolean success) {
        if (success) {
            dispose();
            return;
        }

        JOptionPane.showMessageDialog(UserInfoDialog.this,
                    String.format("Логин %s уже занят", tfLogin.getText()),
                    "Сохранение данных",
                    JOptionPane.ERROR_MESSAGE);
    }
}
