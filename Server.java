import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;

public class Server {
    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            // Tworzenie serwera nasłuchującego na porcie 1410
            server = new ServerSocket(1410);
            server.setReuseAddress(true);
            System.out.println("Serwer uruchomiony i oczekuje na połączenia...");

            while (true) {
                // Akceptowanie połączenia od klienta
                Socket client = server.accept();
                System.out.println("Nowy klient połączony: " + client.getInetAddress().getHostAddress());
                ClientHandler clientSock = new ClientHandler(client);
                new Thread(clientSock).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private SecurityUtils securityUtils;
        private PublicKey clientPublicKey;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;

            try {
                // Inicjalizacja narzędzi kryptograficznych
                securityUtils = new SecurityUtils();
                System.out.println("Inicjalizacja zabezpieczeń dla klienta: " +
                        clientSocket.getInetAddress().getHostAddress());

                ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());

                // Krok 1: Wymiana kluczy publicznych
                clientPublicKey = (PublicKey) objIn.readObject();
                securityUtils.setPartnerPublicKey(clientPublicKey);
                System.out.println("Otrzymano klucz publiczny od klienta");

                objOut.writeObject(securityUtils.getPublicKey());
                System.out.println("Wysłano klucz publiczny do klienta");

                // Krok 2: Uwierzytelnienie klienta
                byte[] challenge = securityUtils.generateChallenge();
                objOut.writeObject(challenge);
                System.out.println("Wysłano wyzwanie do klienta");

                byte[] clientResponse = (byte[]) objIn.readObject();
                boolean clientVerified = securityUtils.verifyChallenge(challenge, clientResponse, clientPublicKey);

                if (!clientVerified) {
                    throw new SecurityException("Nie udało się zweryfikować tożsamości klienta!");
                }
                System.out.println("Zweryfikowano tożsamość klienta");

                // Krok 3: Uwierzytelnienie serwera
                byte[] clientChallenge = (byte[]) objIn.readObject();
                byte[] serverResponse = securityUtils.signChallenge(clientChallenge);
                objOut.writeObject(serverResponse);
                System.out.println("Wysłano odpowiedź na wyzwanie klienta");

                // Krok 4: Wysłanie klucza AES (tylko jeśli uwierzytelnienie się powiodło)
                byte[] encryptedAESKey = securityUtils.encryptAESKey(clientPublicKey);
                objOut.writeObject(encryptedAESKey);
                System.out.println("Wysłano zaszyfrowany klucz AES");

                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Otrzymano wiadomość: " + line);

                    String[] parts = securityUtils.decodeMessage(line);
                    String message = parts[0];
                    byte[] signature = Base64.getDecoder().decode(parts[1]);

                    if (!securityUtils.verify(message, signature, clientPublicKey)) {
                        System.out.println("Ostrzeżenie: Nieprawidłowy podpis wiadomości!");
                        continue;
                    }

                    String[] splitted = message.split("#|:");
                    String response = null;

                    switch (splitted[1]) {
                        case "login": {
                            // Przekazanie wiadomości do serwera logowania
                            try (Socket loginSocket = new Socket("localhost", 2137);
                                 PrintWriter loginOut = new PrintWriter(loginSocket.getOutputStream(), true);
                                 BufferedReader loginIn = new BufferedReader(new InputStreamReader(loginSocket.getInputStream()))) {
                                loginOut.println(message);
                                response = loginIn.readLine();
                                if (response != null) {
                                    byte[] responseSignature = securityUtils.sign(response);
                                    out.println(securityUtils.encodeMessage(response, responseSignature));
                                }
                            }
                            break;
                        }
                        case "register": {
                            // Przekazanie wiadomości do serwera rejestracji
                            try (Socket registerSocket = new Socket("localhost", 2138);
                                 PrintWriter registerOut = new PrintWriter(registerSocket.getOutputStream(), true);
                                 BufferedReader registerIn = new BufferedReader(new InputStreamReader(registerSocket.getInputStream()))) {
                                registerOut.println(message);
                                response = registerIn.readLine();
                                if (response != null) {
                                    byte[] responseSignature = securityUtils.sign(response);
                                    out.println(securityUtils.encodeMessage(response, responseSignature));
                                }
                            }
                            break;
                        }
                        case "tablica": {
                            // Przekazanie wiadomości do serwera tablicy
                            try (Socket tablicaSocket = new Socket("localhost", 2139);
                                 PrintWriter tablicaOut = new PrintWriter(tablicaSocket.getOutputStream(), true);
                                 BufferedReader tablicaIn = new BufferedReader(new InputStreamReader(tablicaSocket.getInputStream()))) {
                                tablicaOut.println(message);
                                response = tablicaIn.readLine();
                                if (response != null) {
                                    byte[] responseSignature = securityUtils.sign(response);
                                    out.println(securityUtils.encodeMessage(response, responseSignature));
                                }
                            }
                            break;
                        }
                        case "chat": {
                            // Przekazanie wiadomości do serwera czatu
                            try (Socket chatSocket = new Socket("localhost", 2120);
                                 PrintWriter chatOut = new PrintWriter(chatSocket.getOutputStream(), true);
                                 BufferedReader chatIn = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()))) {
                                chatOut.println(message);
                                response = chatIn.readLine();
                                if (response != null) {
                                    byte[] responseSignature = securityUtils.sign(response);
                                    out.println(securityUtils.encodeMessage(response, responseSignature));
                                }
                            }
                            break;
                        }
                        case "FTO": {
                            // Przekazanie wiadomości do serwera transferu plików (out)
                            try (Socket FTOSocket = new Socket("localhost", 3333);
                                 PrintWriter FTOOut = new PrintWriter(FTOSocket.getOutputStream(), true);
                                 BufferedReader FTOIn = new BufferedReader(new InputStreamReader(FTOSocket.getInputStream()))) {
                                String packetToSend = message;
                                int packetNo = Integer.parseInt(message.split("#|:")[7]);
                                FTOOut.println(packetToSend);

                                int packetsSended = 1;
                                while (packetsSended < packetNo) {
                                    String nextPacket = in.readLine();
                                    String[] nextParts = securityUtils.decodeMessage(nextPacket);
                                    if (securityUtils.verify(nextParts[0], Base64.getDecoder().decode(nextParts[1]), clientPublicKey)) {
                                        FTOOut.println(nextParts[0]);
                                    }
                                    packetsSended++;
                                }

                                response = FTOIn.readLine();
                                if (response != null) {
                                    byte[] responseSignature = securityUtils.sign(response);
                                    out.println(securityUtils.encodeMessage(response, responseSignature));
                                }
                            }
                            break;
                        }
                        case "FTI": {
                            // Przekazanie wiadomości do serwera transferu plików (in)
                            try (Socket FTISocket = new Socket("localhost", 9921);
                                 PrintWriter FTIOut = new PrintWriter(FTISocket.getOutputStream(), true);
                                 BufferedReader FTIIn = new BufferedReader(new InputStreamReader(FTISocket.getInputStream()))) {
                                FTIOut.println(message);
                                String receivedPacket = FTIIn.readLine();

                                if (receivedPacket != null) {
                                    byte[] responseSignature = securityUtils.sign(receivedPacket);
                                    out.println(securityUtils.encodeMessage(receivedPacket, responseSignature));

                                    int packetNo = Integer.parseInt(receivedPacket.split("#|:")[5]);
                                    int packetsSended = 1;

                                    while (packetsSended < packetNo) {
                                        receivedPacket = FTIIn.readLine();
                                        responseSignature = securityUtils.sign(receivedPacket);
                                        out.println(securityUtils.encodeMessage(receivedPacket, responseSignature));
                                        packetsSended++;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Błąd podczas obsługi klienta: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Zakończono połączenie z klientem: " +
                        clientSocket.getInetAddress().getHostAddress());
            }
        }
    }
}