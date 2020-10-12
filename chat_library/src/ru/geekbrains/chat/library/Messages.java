package ru.geekbrains.chat.library;

public class Messages {

    public static final String DELIMITER = ";";
    public static final String AUTH_REQUEST = "/auth_request";
    public static final String AUTH_ACCEPT = "/auth_accept";
    public static final String AUTH_ERROR_LOGIN = "/auth_error_login";
    public static final String AUTH_ERROR_PASSWORD = "/auth_error_password";
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";
    public static final String BCAST = "/bcast";
    public static final String BCAST_SHORT = "/bcast_short";
    public static final String USER_LIST = "/user_list";
    public static final String CHANGE_NICK = "/change_nick";

    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + login + DELIMITER + password;
    }

    public static String getAuthAccept(String nickname) {
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getAuthErrorLogin(String login) {
        return AUTH_ERROR_LOGIN + DELIMITER + login;
    }

    public static String getAuthErrorPassword(String password) {
        return AUTH_ERROR_PASSWORD + DELIMITER + password;
    }

    public static String getMsgFormatError(String value) {
        return MSG_FORMAT_ERROR + DELIMITER + value;
    }

    public static String getBcast(String src, String value) {
        return BCAST + DELIMITER + System.currentTimeMillis() + DELIMITER + src + DELIMITER + value;
    }

    public static String getBcastShort(String value) {
        return BCAST_SHORT + DELIMITER + value;
    }

    public static String getUserList(String users) {
        return USER_LIST + DELIMITER + users;
    }

    public static String getChangeNick(String newNickname) {
        return CHANGE_NICK + DELIMITER + newNickname;
    }
}