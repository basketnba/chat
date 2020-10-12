package ru.geekbrains.chat.server.core;

import ru.geekbrains.chat.library.Messages;
import ru.geekbrains.network.ServerSocketThread;
import ru.geekbrains.network.ServerSocketThreadListener;
import ru.geekbrains.network.SocketThread;
import ru.geekbrains.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private ServerSocketThread serverSocketThread;
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");
    private final Vector<SocketThread> clients = new Vector<>();
    private final ChatServerListener eventListener;

    public ChatServer(ChatServerListener eventListener) {
        this.eventListener = eventListener;
    }

    public void start(int port) {
        if (serverSocketThread != null && serverSocketThread.isAlive()) {
            putLog("Сервер уже запущен");
            return;
        }
        serverSocketThread = new ServerSocketThread(this, "ServerSocketThread", port, 2000);
        SQLClient.connect();
    }

    public void stop() {
        if (serverSocketThread == null || !serverSocketThread.isAlive()) {
            putLog("Сервер не запущен");
            return;
        }
        serverSocketThread.interrupt();
        SQLClient.disconnect();
    }

    private synchronized void putLog(String msg) {
        msg = dateFormat.format(System.currentTimeMillis()) + Thread.currentThread().getName() + ": " + msg;
        eventListener.onChatServerLog(this, msg);
    }

    //ServerSocketThread
    @Override
    public void onStartServerSocketThread(ServerSocketThread thread) {
        putLog("started...");
    }

    @Override
    public void onStopServerSocketThread(ServerSocketThread thread) {
        putLog("stopped.");
    }

    @Override
    public void onCreateServerSocket(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog("Port listening...");
    }

    @Override
    public void onTimeoutAccept(ServerSocketThread thread, ServerSocket serverSocket) {
//        putLog("timeout accept().");
    }

    @Override
    public void onAcceptedSocket(ServerSocketThread thread, Socket socket) {
        putLog("client connected: " + socket);
        String threadName = "SocketThread: " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, threadName, socket);
    }

    @Override
    public void onServerSocketThreadException(ServerSocketThread thread, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    //SocketThread
    @Override
    public synchronized void onStartSocketThread(SocketThread thread, Socket socket) {
        putLog("started...");
    }

    @Override
    public synchronized void onStopSocketThread(SocketThread thread, Socket socket) {
        putLog("stopped.");
        clients.remove(thread);
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized() && !client.isReconnected()) {
            sendToAllAuthorizedClients(Messages.getBcast("Server", client.getNickname() + " was disconnected"));
            sendToAllAuthorizedClients(Messages.getUserList(getUsers()));
        }
    }

    @Override
    public synchronized void onSocketThreadIsReady(SocketThread thread, Socket socket) {
        putLog("SocketThread is ready");
        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String value) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthorizedMsg(client, value);
        } else {
            handleNonAuthorizedMsg(client, value);
        }
    }

    @Override
    public synchronized void onSocketThreadException(SocketThread thread, Socket socket, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
    }

    private void handleAuthorizedMsg(ClientThread client, String value) {
        String[] arr = value.split(Messages.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Messages.BCAST_SHORT:
                if (arr[1].equals(Messages.CHANGE_NICK)) {
                    String oldNickname = client.getNickname();
                    String newNickname = arr[2];
                    client.setNickname(newNickname);
                    sendToAllAuthorizedClients(Messages.getUserList(getUsers()));
                    client.sendMsg(Messages.getChangeNick(newNickname));
                    SQLClient.setNewNickname(oldNickname, newNickname);
                    return;
                }
                sendToAllAuthorizedClients(Messages.getBcast(client.getNickname(), arr[1]));
                break;
            default:
                client.msgFormatError(value);
        }
    }

    private void handleNonAuthorizedMsg(ClientThread newClient, String value) {
        String[] arr = value.split(Messages.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Messages.AUTH_REQUEST)) {
            newClient.msgFormatError(value);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SQLClient.getNickname(login, password);
        if (nickname == null) {
            if (!SQLClient.isLoginCorrect(login)) {
                putLog("Invalid login = '" + login + "'");
                newClient.authorizeErrorLogin(login);
                return;
            }
            putLog("Invalid password = '" + password + "'");
            newClient.authorizeErrorPassword(password);
            return;
        }
        ClientThread client = findClientByNickname(nickname);
        newClient.authorizeAccept(nickname);
        if (client == null) {
            sendToAllAuthorizedClients(Messages.getBcast("Server", nickname + " was connected"));
        } else {
            client.reconnect();
            clients.remove(client);
        }
        sendToAllAuthorizedClients(Messages.getUserList(getUsers()));
    }

    private void sendToAllAuthorizedClients(String value) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMsg(value);
        }
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Messages.DELIMITER);
        }
        return sb.toString();
    }

    private ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname)) return client;
        }
        return null;
    }
}