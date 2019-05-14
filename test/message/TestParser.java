package message;

import server.authorization.users.User;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static message.MessagePatterns.*;
import static org.junit.Assert.*;

public class TestParser {

    @Test
    public void parseAuthMessageTest() {
        User chatUser = parseAuthMessage("/auth sveta 555");
        assertNotNull(chatUser);
        assertEquals("sveta", chatUser.getLogin());
        assertEquals("555", chatUser.getPassword());
    }

    @Test
    public void parseRegMessageTest() {
        User chatUser = parseRegMessage("/reg sveta 555 Svetlana");
        assertNotNull(chatUser);
        assertEquals("sveta", chatUser.getLogin());
        assertEquals("555", chatUser.getPassword());
        assertEquals("Svetlana", chatUser.getName());
    }

    @Test
    public void parseConnectMessageTest() {
        String login = parseConnectMessage("/connected petr");
        assertEquals("petr", login);
    }

    @Test
    public void parseDisconnectMessageTest() {
        String login = parseDisconnectMessage("/disconnected petr");
        assertEquals("petr", login);
    }

    @Test
    public void parseSendMessageTest() {
        TextMessage textMessage = parseSendMessage("/w ivan petr Hi! Nice to see you!");
        assertNotNull(textMessage);
        assertEquals("ivan", textMessage.getUserTo());
        assertEquals("petr", textMessage.getUserFrom());
        assertEquals("Hi! Nice to see you!", textMessage.getMessage());
    }

    @Test
    public void parseUserListMessageTest() {
        Set<String> userTest = parseUserListMessage("/users [ivan, petr, olga]");

        Set<String> userSet = new HashSet<>();
        userSet.add("ivan");
        userSet.add("petr");
        userSet.add("olga");

        assertEquals(userSet, userTest);
    }

    @Test
    public void parseUserInfoMessageTest() {
        User user = parseUserInfoMessage("/uinfo login password name");
        assertNotNull(user);
        assertEquals("login", user.getLogin());
        assertEquals("password", user.getPassword());
        assertEquals("name", user.getName());

        user = parseUserInfoMessage("/uinfo login password Пётр");
        assertNotNull(user);
        assertEquals("login", user.getLogin());
        assertEquals("password", user.getPassword());
        assertEquals("Пётр", user.getName());
    }

    @Test
    public void parseLoginChangeMessageTest() {
        String[] result = parseLoginChangeMessage("/chlog old new");
        assertNotNull(result);
        assertEquals("old", result[0]);
        assertEquals("new", result[1]);
    }
}