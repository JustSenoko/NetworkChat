package server.authorization;

import server.User;

public interface AuthorizationService {

    User authUser(User user);

    User checkAuthorization(String authMessage);

    User checkRegistration(String regMessage);

    boolean addUser(User user);

    boolean updateUserInfo(String oldLogin, User newUserInfo);
}
