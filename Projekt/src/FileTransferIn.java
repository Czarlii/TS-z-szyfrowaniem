import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.Base64;
import java.util.StringJoiner;
import java.security.NoSuchAlgorithmException;

public class FileTransferIn {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket ftiSocket = new ServerSocket(9921);
        System.out.println("Serwer transferu plików (in) uruchomiony na porcie 9921");
        System.out.println("Oczekiwanie na żądania w katalogu: C:\\Serwer_plikow\\");

        while (true) {
            try {
                Socket clientSocket = ftiSocket.accept();
                System.out.println("\n=== Nowe połączenie od: " + clientSocket.getInetAddress().getHostAddress() + " ===");
                PrintWriter FTIOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader FTIIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String receivedPacket = FTIIn.readLine();
                String requestedFile = receivedPacket.split("#|:")[5];
                System.out.println("Otrzymano żądanie pliku: " + requestedFile);

                String path = "C:\\Serwer_plikow\\" + requestedFile;
                File file = new File(path);
                StringJoiner newFTIpacket = new StringJoiner("#");

                if (!file.exists()) {
                    System.out.println("BŁĄD: Plik nie istnieje: " + requestedFile);
                    newFTIpacket.add("status:NO");
                    newFTIpacket.add("filename:_");
                    newFTIpacket.add("packetsNo:0");
                    newFTIpacket.add("content:_");
                    FTIOut.println(newFTIpacket.toString());
                } else {
                    System.out.println("Znaleziono plik: " + file.getName());
                    System.out.println("Rozmiar pliku: " + file.length() + " bajtów");

                    long fileSize = file.length();
                    int packetCount = (int) Math.ceil((double) fileSize / 1024);
                    System.out.println("Liczba pakietów do wysłania: " + packetCount);

                    byte[] buffer = new byte[1024];
                    InputStream fileInputStream = new FileInputStream(file);
                    int bytesRead;
                    int totalBytesSent = 0;

                    for (int i = 0; i < packetCount; i++) {
                        bytesRead = fileInputStream.read(buffer);
                        totalBytesSent += bytesRead;
                        byte[] packetToAdd = new byte[bytesRead];
                        System.arraycopy(buffer, 0, packetToAdd, 0, bytesRead);
                        newFTIpacket.add("status:OK");
                        newFTIpacket.add("filename:" + file.getName());
                        newFTIpacket.add("packetsNo:" + packetCount);
                        newFTIpacket.add("content:" + Base64.getEncoder().encodeToString(packetToAdd));

                        FTIOut.println(newFTIpacket.toString());
                        System.out.println("Wysłano pakiet " + (i+1) + "/" + packetCount +
                                " (" + totalBytesSent + "/" + fileSize + " bajtów)");
                        newFTIpacket = new StringJoiner("#");
                    }
                    fileInputStream.close();
                    System.out.println("Transfer zakończony pomyślnie");
                }
                System.out.println("=== Zakończono połączenie ===\n");
            } catch (Exception e) {
                System.out.println("BŁĄD podczas transferu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}