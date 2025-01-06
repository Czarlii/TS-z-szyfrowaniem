import java.io.*;
import java.net.*;
import java.sql.*;
import java.security.NoSuchAlgorithmException;

public class Tablica {
    private static SecurityUtils securityUtils;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        // Tworzenie serwera nasłuchującego na porcie 2139
        ServerSocket tablicaSocket = new ServerSocket(2139);
        securityUtils = new SecurityUtils();
        System.out.println("Serwer tablicy uruchomiony na porcie 2139");

        while (true) {
            try {
                // Akceptowanie połączenia od klienta
                Socket clientSocket = tablicaSocket.accept();
                PrintWriter tablicaOut = new PrintWriter(clientSocket.getOutputStream(), true);
                String odpowiedz = "status:OK#tresc:#";

                // Połączenie z bazą danych i pobranie ostatnich 10 wpisów z tablicy
                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/ts", "root", "karol1")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Statement statement = con.createStatement();
                    String loginTworzacego;
                    String trescPosta;
                    ResultSet result = statement.executeQuery("select * from tablica order by id desc LIMIT 10");
                    int count = 0;
                    while (result.next()) {
                        count++;
                        loginTworzacego = result.getString("autor");
                        trescPosta = result.getString("tresc");
                        odpowiedz += loginTworzacego + ":" + trescPosta + "#";
                    }
                    System.out.println("Pobrano " + count + " wpisów z tablicy");
                } catch (Exception e) {
                    e.printStackTrace();
                    odpowiedz = "status:NO";
                }

                // Wysłanie odpowiedzi do klienta
                tablicaOut.println(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}