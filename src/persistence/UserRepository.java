package persistence;

import server.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private Connection connection;

    public UserRepository(Connection connection) throws SQLException {

        this.connection = connection;
        checkUsersTable();
    }

    private void checkUsersTable() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS `users` (\n" +
                "   `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\n" +
                "   `login` varchar(45) DEFAULT NULL,\n" +
                "   `password` varchar(45) DEFAULT NULL,\n" +
                "   `name` varchar(45) DEFAULT NULL,\n" +
                "   PRIMARY KEY (`id`),\n" +
                "   UNIQUE KEY `login_UNIQUE` (`login`)\n" +
                " ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
    }

    public void addUser(User user) throws SQLException {
        // login уникален в SQL, если будет дубль, будет исключение
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO `users` (`login`, `password`, `name`) VALUES (?, ?, ?);");
        ps.setString(1, user.getLogin());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getName());
        ps.execute();

    }

    public User findUserByLogin(String login) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT `password`, `name` FROM `users` WHERE `login` LIKE ?;");
            ps.setString(1, login);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User newUser = new User(login,
                        rs.getString(1),
                        rs.getString(2));
                rs.close();
                return newUser;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        Statement ps = connection.createStatement();
        List<User> res = new ArrayList<>();

        ResultSet rs = ps.executeQuery("SELECT `login`, `password`, `name` FROM `users`;");
        while (rs.next()) {
            res.add(new User(rs.getString(1),
                    rs.getString(2),
                    rs.getString(3)));
        }
        rs.close();
        return res;
    }

    public boolean updateUserInfo(String oldLogin, User newUserInfo) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE `network_chat`.`users` " +
                            "SET `login` = ?, `password` = ?, `name` = ? " +
                            "WHERE (`login` LIKE ?);");
            ps.setString(1, newUserInfo.getLogin());
            ps.setString(2, newUserInfo.getPassword());
            ps.setString(3, newUserInfo.getName());
            ps.setString(4, oldLogin);

            ps.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}