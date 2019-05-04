package authorization;

import authorization.users.User;
import authorization.users.UserRepository;
import message.MessagePatterns;

import java.sql.SQLException;

public class AuthorizationServiceImpl implements AuthorizationService {

    private UserRepository userRepository;

    public AuthorizationServiceImpl(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public boolean authUser(User user) {
        User userDB = userRepository.findUserByLogin(user.getLogin());
        return userDB != null && userDB.getPassword().equals(user.getPassword());
    }

    @Override
    public User checkAuthorization(String authMessage) {
        return MessagePatterns.parseAuthMessage(authMessage);
    }

    @Override
    public User checkRegistration(String regMessage) {
        return MessagePatterns.parseRegMessage(regMessage);
    }

    @Override
    public boolean addUser(User user) {
        if (userRepository.findUserByLogin(user.getLogin()) != null) {
            System.out.printf("User %s already exists%n", user.getLogin());
            return false;
        }
        try {
            userRepository.addUser(user);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        System.out.printf("User %s registered successful!%n", user.getLogin());
        return true;
    }


}
