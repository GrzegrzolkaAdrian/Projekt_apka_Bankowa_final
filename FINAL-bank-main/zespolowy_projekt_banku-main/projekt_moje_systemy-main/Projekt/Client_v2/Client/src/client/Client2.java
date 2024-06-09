package client;

import java.net.*;
import java.io.*;

public class Client2 {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 6698;

        try (Socket clientSocket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader brSockInp = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedReader brLocalInp = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Polaczono z: " + clientSocket);

            Thread listenerThread = new Thread(() -> {
                try {
                    String inputLine;
                    while ((inputLine = brSockInp.readLine()) != null) {
                        System.out.println("[serwer zwraca]: " + inputLine);
                    }
                } catch (IOException e) {
                    System.out.println("Blad czytania z serwera: " + e);
                }
            });
            listenerThread.start();

            System.out.println("Masz mozliwosc wpisywac komendy.");
            String line;
            while ((line = brLocalInp.readLine()) != null) {
                if ("exit".equals(line)) {
                    System.out.println("Zamykanie...");
                    break;
                }
                out.writeBytes(line + "\r\n");
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Blad: " + e.getMessage());
            System.exit(-1);
        }
    }
}
