import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;

public class FileTransferOut {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        // Tworzenie serwera nasłuchującego na porcie 3333
        ServerSocket FTOsocket = new ServerSocket(3333);
        System.out.println("Serwer transferu plików (out) uruchomiony na porcie 3333");
        System.out.println("Katalog docelowy: C:\\Serwer_plikow\\");

        while (true) {
            try {
                // Akceptowanie połączenia od klienta
                Socket clientSocket = FTOsocket.accept();
                System.out.println("\n=== Nowe połączenie od: " + clientSocket.getInetAddress().getHostAddress() + " ===");

                // Inicjalizacja strumieni do komunikacji z klientem
                PrintWriter ftoOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader FTOIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                ArrayList<String> packetsReceivedList = new ArrayList<String>();

                // Odczytanie pierwszego pakietu od klienta
                String receivedPacket = FTOIn.readLine();
                String[] initialPacketInfo = receivedPacket.split("#|:");
                String fileName = initialPacketInfo[5];
                int packetNo = Integer.parseInt(initialPacketInfo[7]);

                System.out.println("Rozpoczęto odbieranie pliku: " + fileName);
                System.out.println("Oczekiwana liczba pakietów: " + packetNo);

                // Odbieranie kolejnych pakietów
                int packetsReceived = 1;
                System.out.println("Odebrano pakiet: 1/" + packetNo);

                while (packetsReceived < packetNo) {
                    packetsReceivedList.add(receivedPacket);
                    receivedPacket = FTOIn.readLine();
                    packetsReceived++;
                    System.out.println("Odebrano pakiet: " + packetsReceived + "/" + packetNo);
                }
                if (packetNo == 1) {
                    packetsReceivedList.add(receivedPacket);
                }
                System.out.println("Odebrano wszystkie pakiety. Rozpoczynam składanie pliku...");

                // Składanie pliku z odebranych pakietów
                String[] splittedPacket;
                String newFileName = "";
                ArrayList<byte[]> newFileContentList = new ArrayList<>();
                long totalSize = 0;

                for (int i = 0; i < packetsReceivedList.size(); i++) {
                    splittedPacket = packetsReceivedList.get(i).split("#|:");
                    byte[] receivedContent = Base64.getDecoder().decode(splittedPacket[9]);
                    totalSize += receivedContent.length;
                    newFileContentList.add(receivedContent);
                    if (i == 0) {
                        newFileName = splittedPacket[5];
                    }
                }

                // Zapisanie pliku na serwerze
                String newPath = "C:\\Serwer_plikow\\" + newFileName;
                File file = new File(newPath);
                String response;
                boolean isExisting = file.exists();

                if (newFileName.equals("")) {
                    response = "status:NO";
                    System.out.println("BŁĄD: Otrzymano pustą nazwę pliku");
                } else if (isExisting) {
                    response = "status:NO";
                    System.out.println("BŁĄD: Plik już istnieje w lokalizacji: " + newPath);
                } else {
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        for (byte[] content : newFileContentList) {
                            outputStream.write(content);
                        }
                        response = "status:OK";
                        System.out.println("Sukces: Zapisano plik " + newFileName);
                        System.out.println("Lokalizacja: " + newPath);
                        System.out.println("Rozmiar: " + totalSize + " bajtów");
                    } catch (IOException e) {
                        response = "status:NO";
                        System.out.println("BŁĄD podczas zapisywania pliku: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Wysłanie odpowiedzi do klienta
                ftoOut.println(response);
                System.out.println("=== Zakończono połączenie ===\n");
            } catch (Exception e) {
                // Obsługa błędów podczas obsługi połączenia
                System.out.println("BŁĄD podczas obsługi połączenia: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}