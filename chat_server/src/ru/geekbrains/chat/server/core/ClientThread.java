package ru.geekbrains.chat.server.core;

import ru.geekbrains.chat.library.Messages;
import ru.geekbrains.network.SocketThread;
import ru.geekbrains.network.SocketThreadListener;

import java.net.Socket;

class ClientThread extends SocketThread {

    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnected;

    ClientThread(SocketThreadListener eventListener, String name, Socket socket) {
        super(eventListener, name, socket);
    }

    void authorizeAccept(String nickname) {
        isAuthorized = true;
        this.nickname = nickname;
        sendMsg(Messages.getAuthAccept(nickname));
    }

    void authorizeErrorLogin(String login) {
        sendMsg(Messages.getAuthErrorLogin(login));
        close();
    }

    void authorizeErrorPassword(String password) {
        sendMsg(Messages.getAuthErrorPassword(password));
        close();
    }

    void msgFormatError(String value) {
        sendMsg(Messages.getMsgFormatError(value));
        close();
    }

    void reconnect() {
        isReconnected = true;
        close();
    }

    String getNickname() {
        return nickname;
    }

    void setNickname(String nickname) {
        this.nickname = nickname;
    }

    boolean isAuthorized() {
        return isAuthorized;
    }

    boolean isReconnected() {
        return isReconnected;
    }
}