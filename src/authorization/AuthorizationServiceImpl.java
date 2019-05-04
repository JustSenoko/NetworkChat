package authorization;

import message.MessagePatterns;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthorizationServiceImpl implements AuthorizationService {

    private Map<String, ChatUser> users = Collections.synchronizedMap(new HashMap<>());

    public AuthorizationServiceImpl() {
        users.put("ivan", new ChatUser("ivan", "123", "Иван"));
        users.put("petr", new ChatUser("petr", "345", "Петр"));
        users.put("julia", new ChatUser("", "789", "Юлия"));
    }

    @Override
    public boolean authUser(ChatUser user) {
        String pwd = users.get(user.getLogin()).getPassword();
        return pwd != null && pwd.equals(user.getPassword());
    }

    @Override
    public ChatUser checkAuthorization(String authMessage) {
        return MessagePatterns.parseAuthMessage(authMessage);
    }

    @Override
    public ChatUser checkRegistration(String regMessage) {
        return MessagePatterns.parseRegMessage(regMessage);
    }

    @Override
    public boolean addUser(ChatUser user) {
        if (users.containsKey(user.getLogin())) {
            return false;
        }
        users.put(user.getLogin(), user);
        System.out.printf("User %s registered successful!%n", user.getLogin());
        return true;
    }


}
