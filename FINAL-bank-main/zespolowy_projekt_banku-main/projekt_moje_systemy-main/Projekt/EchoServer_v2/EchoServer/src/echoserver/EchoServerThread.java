package echoserver;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServerThread implements Runnable {
    private Socket socket;
    private CopyOnWriteArrayList<ArrayList<String>> logins;
    private int IndeksUzytkownika = -1;

    public EchoServerThread(Socket clientSocket, CopyOnWriteArrayList<ArrayList<String>> logins) {
        this.socket = clientSocket;
        this.logins = logins;
    }

    @Override
    public void run() {
        try (BufferedReader brinp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String nazwaWatku = Thread.currentThread().getName();
            String linia;

            while ((linia = brinp.readLine()) != null) {
                System.out.println(nazwaWatku + "| Line read: " + linia);

                if (linia == "zaloguj")
                {
                    Logowanie(brinp, out, nazwaWatku);
                }
                else if (linia == "rejestracja")
                {
                    Rejestrowanie(brinp, out, nazwaWatku);
                }
                else if (linia == "wylistuj")
                {
                    Wylistuj(out);
                }
                else if (linia == "depozyt")
                {
                    Depozyt(brinp, out);
                }
                else if (linia == "wyplata")
                {
                    Wyplata(brinp, out);
                }
                else if (linia == "transfer")
                {
                    Transfer(brinp, out);
                }
                else if (linia == "balans")
                {
                    Balans(out);
                }
                else
                {
                    out.writeBytes("Niepoprawne polecenie.\r\n");
                    System.out.println(nazwaWatku + "| Niepoprawne polecenie: " + linia);
                }

            }
        } catch (IOException e) {
            System.out.println("Error, " + e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Niepoprawne zamkniecie socketu: " + e);
            }
        }
    }

    private void Logowanie(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        boolean loginPassword = false;
        out.writeBytes("Podaj login i haslo\r\n");
        while (!loginPassword) {
            String LiniaDanych = brinp.readLine();
            if (LiniaDanych == null) return;
            String[] DaneLogowania = LiniaDanych.split(" ");
            for (int i = 0; i < logins.size(); i++) {
                if (logins.get(i).get(0).equals(DaneLogowania[0]) && logins.get(i).get(1).equals(DaneLogowania[1])) {
                    IndeksUzytkownika = i;
                    loginPassword = true;
                    out.writeBytes("Dane sie zgadzaja, zalogowano. Twoje saldo wynosi: " + logins.get(i).get(3) + "\r\n");
                    break;
                }
            }
            if (!loginPassword) {
                out.writeBytes("Dane sa niepoprawne. Sprobuj ponownie.\r\n");
            }
        }
    }

    private void Wylistuj(DataOutputStream out) throws IOException {
        StringBuilder userList = new StringBuilder();
        for (ArrayList<String> DataUzytkownika : logins) {
            userList.append(DataUzytkownika.get(2)).append("\r\n");
        }
        out.writeBytes(userList.toString());
        out.flush();
    }

    private void Balans(DataOutputStream out) throws IOException {
        if (IndeksUzytkownika == -1) {
            out.write("Nie jestes zalogowany, przed wykonaniem operacji prosze sie zalogowac.\r\n".getBytes("UTF-8"));
            return;
        }
        double ObecneSaldo = Double.parseDouble(logins.get(IndeksUzytkownika).get(3));
        out.write(("Twoje obecne saldo wynosi: " + String.format(Locale.US, "%.2f", ObecneSaldo) + "\r\n").getBytes("UTF-8"));
    }

    private void Depozyt(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (IndeksUzytkownika == -1) {
            out.write("Nie jestes zalogowany, przed wykonaniem operacji prosze sie zalogowac.\r\n".getBytes("UTF-8"));
            return;
        }
        out.write("Jaka kwote chcesz zdeponowac:\r\n".getBytes("UTF-8"));
        String kwotaStr = brinp.readLine();
        try {
            double kwota = Double.parseDouble(kwotaStr);
            if(kwota < 1)
            {
                out.write("Nie mozesz zdeponowac takiej kwoty!\r\n".getBytes("UTF-8"));
                return;
            }
            double ObecneSaldo = Double.parseDouble(logins.get(IndeksUzytkownika).get(3));
            ObecneSaldo += kwota;
            logins.get(IndeksUzytkownika).set(3, String.format(Locale.US, "%.2f", ObecneSaldo));
            zapisDoBazy();
            out.write(("Nowe saldo wynosi: " + String.format(Locale.US, "%.2f", ObecneSaldo) + "\r\n").getBytes("UTF-8"));
        } catch (NumberFormatException e) {
            out.write("Format liczby jest niepoprawny.\r\n".getBytes("UTF-8"));
        }
    }

    private void Wyplata(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (IndeksUzytkownika == -1) {
            out.write("Nie jestes zalogowany, przed wykonaniem operacji prosze sie zalogowac.\r\n".getBytes("UTF-8"));
            return;
        }
        out.write("Jaka kwote chcesz wyplacic:\r\n".getBytes("UTF-8"));
        String kwotaStr = brinp.readLine();
        try {
            double kwota = Double.parseDouble(kwotaStr);
            if(kwota <= 0)
            {
                out.write("Nie mozesz wyplacic takiej kwoty!\r\n".getBytes("UTF-8"));
                return;
            }
            double ObecneSaldo = Double.parseDouble(logins.get(IndeksUzytkownika).get(3));
            if (kwota <= ObecneSaldo) {
                ObecneSaldo -= kwota;
                logins.get(IndeksUzytkownika).set(3, String.format(Locale.US, "%.2f", ObecneSaldo));
                zapisDoBazy();
                out.write(("Nowe saldo wynosi: " + String.format(Locale.US, "%.2f", ObecneSaldo) + "\r\n").getBytes("UTF-8"));
            } else {
                out.write("Masz niewystarczajace srodki.\r\n".getBytes("UTF-8"));
            }
        } catch (NumberFormatException e) {
            out.write("Format liczby jest niepoprawny.\r\n".getBytes("UTF-8"));
        }
    }

    private void Transfer(BufferedReader brinp, DataOutputStream out) throws IOException {
        if (IndeksUzytkownika == -1) {
            out.write("Nie jestes zalogowany, przed wykonaniem operacji prosze sie zalogowac.\r\n".getBytes("UTF-8"));
            return;
        }
        out.write("Podaj login na ktory chcesz wyslac przelew oraz kwote przelewu: \r\n".getBytes("UTF-8"));
        String line = brinp.readLine();
        if (line == null) return;

        String[] DaneTransferu = line.split(" ");
        if (DaneTransferu.length < 2) {
            out.write("Nieprawidlowe dane.\r\n".getBytes("UTF-8"));
            return;
        }

        String LoginOdbiorcy = DaneTransferu[0];
        double kwota;


        try {
            kwota = Double.parseDouble(DaneTransferu[1]);
        } catch (NumberFormatException e) {
            out.write("Format liczby jest niepoprawny.\r\n".getBytes("UTF-8"));
            return;
        }

        if(kwota < 1)
        {
            out.write("Nie mozesz wyslac takiej kwoty!\r\n".getBytes("UTF-8"));
            return;
        }

        int IndeksOdbiorcy = -1;
        for (int i = 0; i < logins.size(); i++) {
            if (logins.get(i).get(0).equals(LoginOdbiorcy)) {
                IndeksOdbiorcy = i;
                break;
            }
        }

        if (IndeksOdbiorcy != -1 && kwota <= Double.parseDouble(logins.get(IndeksUzytkownika).get(3))) {
            double SaldoNadawcy = Double.parseDouble(logins.get(IndeksUzytkownika).get(3)) - kwota;
            double SaldoOdbiorcy = Double.parseDouble(logins.get(IndeksOdbiorcy).get(3)) + kwota;
            logins.get(IndeksUzytkownika).set(3, String.format(Locale.US, "%.2f", SaldoNadawcy));
            logins.get(IndeksOdbiorcy).set(3, String.format(Locale.US, "%.2f", SaldoOdbiorcy));
            zapisDoBazy();
            out.write(("Przelew zostal wyslany poprawnie, nowe saldo wynosi: " + String.format(Locale.US, "%.2f", SaldoNadawcy) + "\r\n").getBytes("UTF-8"));
        } else {
            out.write("Przelew sie nie powiodl, niewystarczajace srodki lub nie znaleziono Odbiorcy.\r\n".getBytes("UTF-8"));
        }
    }

    private void Rejestrowanie(BufferedReader brinp, DataOutputStream out, String threadName) throws IOException {
        out.writeBytes("Login, haslo, nazwa uzytkownika?\r\n");
        String linia = brinp.readLine();
        if (linia == null) return;
        String[] Czesc = linia.split(" ");
        for (ArrayList<String> DataUzytkownika : logins) {
            if (DataUzytkownika.get(0).equals(Czesc[0])) {
                out.writeBytes("Ten login jest zajety, wybierz inny.\r\n");
                return;
            }
        }
        ArrayList<String> nowyUzytkownik = new ArrayList<>();
        nowyUzytkownik.add(Czesc[0]);
        nowyUzytkownik.add(Czesc[1]);
        nowyUzytkownik.add(Czesc[2]);
        nowyUzytkownik.add("0.00");
        logins.add(nowyUzytkownik);
        zapisDoBazy();
        out.writeBytes("Uzytkownik zostal zarejestrowany pomyslnie.\r\n");
    }

    private void zapisDoBazy() {
        Path path = Paths.get("BazaDanych.txt");
        List<String> lines = new ArrayList<>();
        for (ArrayList<String> userDetails : logins) {
            String line = String.join(",", userDetails);
            lines.add(line);
        }
        try {
            Files.write(path, lines);
        } catch (IOException e) {
            System.out.println("Blad zapisu danych: " + e);
        }
    }
}
