package ru.geekbrains.chat.client;

import ru.geekbrains.chat.library.Messages;
import ru.geekbrains.network.SocketThread;
import ru.geekbrains.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Scanner;

class ClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientGUI();
            }
        });
    }

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;

    private final JTextArea log = new JTextArea();

    private final JPanel upperPanel = new JPanel(new GridLayout(2, 3));
    private final JTextField fieldIPAddr = new JTextField("127.0.0.1");
    private final JTextField fieldPort = new JTextField("8189");
    private final JCheckBox chkAlwaysOnTop = new JCheckBox("Always on top", true);
    private final JTextField fieldLogin = new JTextField("root");
    private final JPasswordField fieldPass = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");

    private final JPanel bottomPanel = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField fieldInput = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();

    private static final String PATH = "history.txt";
    private static final String TITLE = "Chat Client";
    private static final String[] EMPTY = new String[0];
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);
        setLocationRelativeTo(null);

        fieldIPAddr.addActionListener(this);
        fieldPort.addActionListener(this);
        fieldLogin.addActionListener(this);
        fieldPass.addActionListener(this);
        btnLogin.addActionListener(this);
        fieldInput.addActionListener(this);
        btnSend.addActionListener(this);
        chkAlwaysOnTop.addActionListener(this);
        btnDisconnect.addActionListener(this);
        setAlwaysOnTop(chkAlwaysOnTop.isSelected());

        upperPanel.add(fieldIPAddr);
        upperPanel.add(fieldPort);
        upperPanel.add(chkAlwaysOnTop);
        upperPanel.add(fieldLogin);
        upperPanel.add(fieldPass);
        upperPanel.add(btnLogin);
        add(upperPanel, BorderLayout.NORTH);

        bottomPanel.add(btnDisconnect, BorderLayout.WEST);
        bottomPanel.add(fieldInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        JScrollPane scrollLog = new JScrollPane(log);
        log.setEditable(false);
        log.setLineWrap(true);
        readMsg();
        add(scrollLog, BorderLayout.CENTER);

        JScrollPane scrollUsers = new JScrollPane(userList);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        add(scrollUsers, BorderLayout.EAST);

        bottomPanel.setVisible(false);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == chkAlwaysOnTop) {
            setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == fieldInput) {
            sendMsg();
        } else if (src == fieldIPAddr || src == fieldPort || src == fieldLogin || src == fieldPass || src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else {
            throw new RuntimeException("Неизвестный src = " + src);
        }
    }

    private SocketThread socketThread;

    private void connect() {
        try {
            Socket socket = new Socket(fieldIPAddr.getText(), Integer.parseInt(fieldPort.getText()));
            socketThread = new SocketThread(this, "SocketThread", socket);
        } catch (IOException e) {
            log.append("Exception: " + e.getMessage() + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        }
    }

    private void readMsg() {
        File file = new File(PATH);
        if (file.length() == 0L) return;
        try (Scanner scanner = new Scanner(new FileInputStream(file), Charset.forName("UTF-8"))) {
            String line = scanner.nextLine();
            if (line.equals("﻿")) return;
            log.append(line + "\n");
            while (scanner.hasNextLine()) {
                log.append(scanner.nextLine() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMsg() {
        String msg = fieldInput.getText();
        if (msg.trim().length() == 0) {
            fieldInput.grabFocus();
            return;
        }
        fieldInput.setText(null);
        fieldInput.grabFocus();
        socketThread.sendMsg(Messages.getBcastShort(msg));
        try (PrintWriter pw = new PrintWriter(new FileWriter(PATH, true))) {
            pw.print(msg);
            pw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        String msg;
        if (stackTraceElements.length == 0) {
            msg = "Пустой stackTrace";
        } else {
            msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\n" + stackTraceElements[0];
        }
        JOptionPane.showMessageDialog(this, msg, "Exception: ", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    //SocketThread
    @Override
    public void onStartSocketThread(SocketThread thread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Поток сокета запущен.\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    @Override
    public void onStopSocketThread(SocketThread thread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Соединение потеряно.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(true);
                bottomPanel.setVisible(false);
                setTitle(TITLE);
                userList.setListData(EMPTY);
            }
        });
    }

    @Override
    public void onSocketThreadIsReady(SocketThread thread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Соединение установлено.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(false);
                bottomPanel.setVisible(true);
                String login = fieldLogin.getText();
                String password = new String(fieldPass.getPassword());
                socketThread.sendMsg(Messages.getAuthRequest(login, password));
            }
        });
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String value) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                handleMessage(value);
            }
        });
    }

    @Override
    public void onSocketThreadException(SocketThread thread, Socket socket, Exception e) {
        e.printStackTrace();
    }

    private void handleMessage(String value) {
        String[] arr = value.split(Messages.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Messages.AUTH_ACCEPT:
            case Messages.CHANGE_NICK:
                setTitle(TITLE + " вход под ником: " + arr[1]);
                break;
            case Messages.AUTH_ERROR_LOGIN:
                log.append("Неверный login: " + arr[1] + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.AUTH_ERROR_PASSWORD:
                log.append("Неверный password: " + arr[1] + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.MSG_FORMAT_ERROR:
                log.append(value + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                socketThread.close();
                break;
            case Messages.BCAST:
                log.append(dateFormat.format(Long.parseLong(arr[1])) + arr[2] + ": " + arr[3] + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.USER_LIST:
                String users = value.substring(Messages.USER_LIST.length() + Messages.DELIMITER.length());
                String[] usersArr = users.split(Messages.DELIMITER);
                Arrays.sort(usersArr);
                userList.setListData(usersArr);
                break;
            default:
                throw new RuntimeException("Неизвестный тип сообщения: " + msgType);
        }
    }
}