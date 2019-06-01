package server.authorization;

import message.MessagePatterns;
import persistence.UserRepository;
import server.User;

import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;
    private static final Logger LOGGER = Logger.getLogger(AuthorizationServiceImpl.class.getName());

    public AuthorizationServiceImpl(UserRepository userRepository, FileHandler fileHandler) {

        this.userRepository = userRepository;
        LOGGER.addHandler(fileHandler);
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
            LOGGER.warning(String.format("User %s already exists%n", user.getLogin()));
            return false;
        }
        try {
            userRepository.addUser(user);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        LOGGER.info(String.format("User %s registered successful!%n", user.getLogin()));
        return true;
    }

    @Override
    public boolean updateUserInfo(String oldLogin, User newUserInfo) {
        return userRepository.updateUserInfo(oldLogin, newUserInfo);
    }


}
