import java.io.*;
import java.net.*;
import java.sql.*;
import java.security.NoSuchAlgorithmException;

public class Login {
    private static SecurityUtils securityUtils;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        // Tworzenie serwera nasłuchującego na porcie 2137
        ServerSocket loginSocket = new ServerSocket(2137);
        securityUtils = new SecurityUtils();
        System.out.println("Serwer logowania uruchomiony na porcie 2137");

        while (true) {
            try {
                // Akceptowanie połączenia od klienta
                Socket clientSocket = loginSocket.accept();
                PrintWriter loginOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader loginIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Odczytanie zakodowanej wiadomości od klienta
                String encodedMessage = loginIn.readLine();
                String[] parts = encodedMessage.split("#|:");

                // Login i hasło są już w formie niezaszyfrowanej, bo Server.java je rozszyfrował
                String login = parts[3];
                String haslo = parts[5];
                String odpowiedz;

                // Połączenie z bazą danych i weryfikacja użytkownika
                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/ts", "root", "karol1")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    PreparedStatement statement = con.prepareStatement("select * from users where login=? and haslo=?");
                    statement.setString(1, login);
                    statement.setString(2, haslo);
                    ResultSet result = statement.executeQuery();

                    // Sprawdzenie, czy użytkownik istnieje w bazie danych
                    if (result.next()) {
                        odpowiedz = "status:OK";
                        System.out.println("Udane logowanie dla: " + login);
                    } else {
                        odpowiedz = "status:NO";
                        System.out.println("Nieudane logowanie dla: " + login);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    odpowiedz = "status:NO";
                }

                // Wysłanie odpowiedzi do klienta
                loginOut.println(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}