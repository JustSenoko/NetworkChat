package authorization.users;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private Connection connection;

    public UserRepository(Connection connection) {

        this.connection = connection;
    }

    public void addUser(User user) throws SQLException {
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
}