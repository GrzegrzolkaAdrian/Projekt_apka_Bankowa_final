package echoserver;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServer {

    private static void WczytajDaneUzytkownika(CopyOnWriteArrayList<ArrayList<String>> logins) {
        Path path = Paths.get("BazaDanych.txt");
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String[] data = line.split(",");
                ArrayList<String> userDetails = new ArrayList<>(Arrays.asList(data));
                logins.add(userDetails);
            }
        } catch (IOException e) {
            System.out.println("Blad czytania danych z uzytkownika, " + e);
        }
    }

    public static void main(String[] args) {
        CopyOnWriteArrayList<ArrayList<String>> logins = new CopyOnWriteArrayList<>();
        WczytajDaneUzytkownika(logins);

        try (ServerSocket serverSocket = new ServerSocket(6698)) {
            System.out.println("Socket zinicjalizowany...");
            System.out.println("Parametry Socket'u: " + serverSocket);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Polaczenie Nawiazano...");
                System.out.println("Parametry Polaczenia: " + socket);
                new Thread(new EchoServerThread(socket, logins)).start();
            }
        } catch (IOException e) {
            System.out.println("Blad ustawiania Socket'u: " + e);
            System.exit(-1);
        }
    }
}
