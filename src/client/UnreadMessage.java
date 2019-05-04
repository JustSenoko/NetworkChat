package client;

public class UnreadMessage {
    private String login;
    private int unreadCount;

    public UnreadMessage(String login) {
        this.login = login;
        this.unreadCount = 0;
    }

    public String getLogin() {
        return login;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void incUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
    }
}
