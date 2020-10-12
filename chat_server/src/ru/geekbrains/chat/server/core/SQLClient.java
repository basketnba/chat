package ru.geekbrains.chat.server.core;

import java.sql.*;

class SQLClient {

    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat_db.sqlite");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static String getNickname(String login, String password) {
        String request = "SELECT nickname FROM users WHERE login='" + login + "' AND password='" + password + "'";
        try (ResultSet resultSet = statement.executeQuery(request)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static boolean isLoginCorrect(String login) {
        String request = "SELECT login FROM users WHERE login='" + login + "'";
        try (ResultSet resultSet = statement.executeQuery(request)) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void setNewNickname(String oldNickname, String newNickname) {
        String request = "UPDATE users SET nickname='" + newNickname + "' WHERE nickname='" + oldNickname + "'";
        try {
            statement.executeUpdate(request);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}