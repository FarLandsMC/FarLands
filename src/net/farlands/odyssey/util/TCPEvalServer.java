package net.farlands.odyssey.util;

import net.farlands.odyssey.FarLands;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TCPEvalServer {

    private final BiFunction<String, String, String> handler;
    private int port;

    private boolean serverRunning;
    private ServerSocket serverSocket;
    private Thread serverThread;

    public TCPEvalServer(BiFunction<String, String, String> handler) {
        this.handler = handler;
    }

    public synchronized boolean start(int port) {
        if (serverRunning && serverSocket != null && !serverSocket.isClosed())
            return false;

        this.port = port;
        stop();

        try {
            serverSocket = new ServerSocket(port, 100, new InetSocketAddress(0).getAddress());
            serverRunning = true;

            serverThread = new Thread(() -> {
                while (serverRunning) {
                    try {
                        final Socket activeSocket = serverSocket.accept();
                        Runnable runnable = () -> handleRequest(activeSocket);
                        new Thread(runnable).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            serverThread.start();
        } catch (IOException e) {
            FarLands.error("Failed to start TCPEvalServer.");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String status() {
        return (serverRunning && serverSocket != null && !serverSocket.isClosed())
                ? String.valueOf(port)
                : null;
    }

    public synchronized boolean stop() {
        if (!serverRunning && serverSocket == null || serverSocket.isClosed())
            return false;

        serverRunning = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        serverSocket = null;
        serverThread = null;

        return true;
    }

    private void handleRequest(Socket socket) {
        try {

            BufferedReader socketReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            BufferedWriter socketWriter = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );

            String token = "";
            boolean recording = false;
            List<String> buffer = new ArrayList<>();

            String line;

            while ((line = socketReader.readLine()) != null) {

                if (line.startsWith("@")) {
                    String[] split = line.substring(1, line.length()).split(" ");
                    String command = split[0].toLowerCase();
                    if (command.equals("auth") && split.length > 1) {
                        token = split[1];
                        recording = true;
                    } else if (command.equals("eval") && recording) {
                        if (!buffer.isEmpty()) {
                            socketWriter.write(handler.apply(token, String.join("\n", buffer)));
                            socketWriter.write("\n");
                            socketWriter.flush();
                            buffer.clear();
                        }
                        recording = false;
                    } else if (command.equals("clear")) {
                        buffer.clear();
                    }
                } else {
                    if (recording)
                        buffer.add(line);
                }

            }

            socket.close();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }

}
