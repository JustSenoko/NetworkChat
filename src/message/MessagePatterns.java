package message;

import authorization.users.User;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessagePatterns {

    private static final String AUTH_PREFIX = "/auth";
    public static final String AUTH_SEND_PATTERN = AUTH_PREFIX + " %s %s";
    private static final Pattern AUTH_REC_PATTERN = Pattern.compile(
            patternConstructor(AUTH_PREFIX, "(\\w+) (\\w+)"));      // /auth login password
    private static final String AUTH_RESULT_PATTERN = AUTH_PREFIX + " %s";// /auth login successful

    private static final String SUCCESS = "successful";
    private static final String FAIL = "fails";

    private static final String REG_PREFIX = "/reg";
    public static final String REG_SEND_PATTERN = REG_PREFIX + " %s %s %s";
    private static final Pattern REG_REC_PATTERN = Pattern.compile(
            patternConstructor(REG_PREFIX, "(\\w+) (\\w+) (\\w+)"));  // /reg login password name
    private static final String REG_RESULT_PATTERN = REG_PREFIX + " %s";

    public static final String CONNECTED = "/connected";
    public static final String CONNECTED_SEND = CONNECTED + " %s";
    private static final Pattern CONNECTED_REC;

    static {
        CONNECTED_REC = Pattern.compile(patternConstructor(CONNECTED, "(\\w+)"));
    }

    public static final String DISCONNECTED = "/disconnected";
    public static final String DISCONNECTED_SEND = DISCONNECTED + " %s";
    private static final Pattern DISCONNECTED_REC = Pattern.compile(patternConstructor(DISCONNECTED, "(\\w+)"));


    public static final String MESSAGE_PREFIX = "/w";
    public static final String MESSAGE_SEND_PATTERN = "/w %s %s %s";
    private static final Pattern MESSAGE_REC_PATTERN = Pattern.compile(
            patternConstructor(MESSAGE_PREFIX, "(\\w+) (\\w+) (.+)"), Pattern.MULTILINE);

    public static final String USERS_PREFIX = "/users";
    public static final String USERS_PATTERN = USERS_PREFIX + " %s"; // / users [login1, login2, ...]
    private static final Pattern USERS_REC_PATTERN = Pattern.compile(
            patternConstructor(USERS_PREFIX, "\\[(.+)]"));

    public static final String EX_MESSAGE_PATTERN = "Unknown message pattern: %s%n";

    private static String patternConstructor(String prefix, String args) {
        return String.format("^%s%s%s", prefix, (args.equals("") ? "" : " "), args);
    }

    public static User parseAuthMessage(String msg) {
        Matcher matcher = AUTH_REC_PATTERN.matcher(msg);
        if (matcher.matches()) {
            return new User(matcher.group(1), matcher.group(2), null);
        } else {
            System.out.printf(EX_MESSAGE_PATTERN, msg);
            return null;
        }
    }

    public static String authResult(boolean result) {
        String res = (result ? SUCCESS : FAIL);
        return String.format(AUTH_RESULT_PATTERN, res);
    }

    public static User parseRegMessage(String msg) {
        Matcher matcher = REG_REC_PATTERN.matcher(msg);
        if (matcher.matches()) {
            return new User(matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            System.out.printf(EX_MESSAGE_PATTERN, msg);
            return null;
        }
    }

    public static String regResult(boolean result) {
        String res = (result ? SUCCESS : FAIL);
        return String.format(REG_RESULT_PATTERN, res);
    }

    public static String parseConnectMessage(String msg) {
        return StringParameter(CONNECTED_REC, msg);
    }

    public static String parseDisconnectMessage(String msg) {
        return StringParameter(DISCONNECTED_REC, msg);
    }

    public static TextMessage parseSendMessage(String msg) {
        Matcher matcher = MESSAGE_REC_PATTERN.matcher(msg);
        if (matcher.matches()) {
            return new TextMessage(matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            System.out.println("Unknown message pattern: " + msg);
            return null;
        }
    }

    public static Set<String> parseUserListMessage(String msg) {
        Matcher matcher = USERS_REC_PATTERN.matcher(msg);
        if (matcher.matches()) {
            String userList = matcher.group(1);
            Pattern loginPattern = Pattern.compile("(\\w+)");
            matcher = loginPattern.matcher(userList);
            Set<String> result = new HashSet<>();

            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            if (result.size() > 0) {
                return result;
            }
        } else {
            System.out.println("Unknown message pattern: " + msg);
        }
        return null;
    }

    private static String StringParameter(Pattern pattern, String msg) {
        Matcher matcher = pattern.matcher(msg);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            System.out.printf(EX_MESSAGE_PATTERN, msg);
            return null;
        }
    }
}
