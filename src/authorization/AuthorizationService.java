package authorization;

import authorization.users.User;

public interface AuthorizationService {

    boolean authUser(User user);

    User checkAuthorization(String authMessage);

    User checkRegistration(String regMessage);

    boolean addUser(User user);
}
