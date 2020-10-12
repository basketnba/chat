package ru.geekbrains.network;

import java.net.Socket;

public interface SocketThreadListener {

    void onStartSocketThread(SocketThread thread, Socket socket);

    void onStopSocketThread(SocketThread thread, Socket socket);

    void onSocketThreadIsReady(SocketThread thread, Socket socket);

    void onReceiveString(SocketThread thread, Socket socket, String value);

    void onSocketThreadException(SocketThread thread, Socket socket, Exception e);
}