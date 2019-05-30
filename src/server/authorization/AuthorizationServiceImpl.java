package server.authorization;

import persistence.UserRepository;
import message.MessagePatterns;
import server.User;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;
    private final Logger logger;
    private final String className = AuthorizationServiceImpl.class.getName();

    public AuthorizationServiceImpl(UserRepository userRepository, Logger logger) {

        this.userRepository = userRepository;
        this.logger = logger;
    }

    @Override
    public User authUser(User user) {
        User userDB = userRepository.findUserByLogin(user.getLogin());

        if (userDB != null && userDB.getPassword().equals(user.getPassword())) {
            return userDB;
        }
        return null;
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
            logger.logp(Level.WARNING,
                    className, "addUser",
                    String.format("User %s already exists%n", user.getLogin()));
            return false;
        }
        try {
            userRepository.addUser(user);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        logger.logp(Level.INFO,
                className, "addUser",
                String.format("User %s registered successful!%n", user.getLogin()));
        return true;
    }

    @Override
    public boolean updateUserInfo(String oldLogin, User newUserInfo) {
        return userRepository.updateUserInfo(oldLogin, newUserInfo);
    }


}
