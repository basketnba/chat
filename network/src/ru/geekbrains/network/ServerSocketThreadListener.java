package ru.geekbrains.network;

import java.net.ServerSocket;
import java.net.Socket;

public interface ServerSocketThreadListener {

    void onStartServerSocketThread(ServerSocketThread thread);

    void onStopServerSocketThread(ServerSocketThread thread);

    void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket);

    void onTimeoutAccept(ServerSocketThread thread, ServerSocket serverSocket);

    void onAcceptedSocket(ServerSocketThread thread, Socket socket);

    void onServerSocketThreadException(ServerSocketThread thread, Exception e);
}