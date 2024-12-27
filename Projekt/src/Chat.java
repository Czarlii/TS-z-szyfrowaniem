import java.io.*;
import java.net.*;
import java.sql.*;
import java.security.NoSuchAlgorithmException;

public class Chat {
    private static SecurityUtils securityUtils;

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        ServerSocket chatSocket = new ServerSocket(2120);
        securityUtils = new SecurityUtils();
        System.out.println("Serwer czatu uruchomiony na porcie 2120");

        while (true) {
            try {
                Socket clientSocket = chatSocket.accept();
                PrintWriter chatOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader chatIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String message = chatIn.readLine();
                System.out.println("Otrzymano wiadomość: " + message);

                // Wiadomość jest już odszyfrowana przez Server.java
                String[] parts = message.split("#|:");
                String odpowiedz;

                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/ts", "root", "karol1")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    PreparedStatement insertStmt = con.prepareStatement("INSERT INTO tablica (autor, tresc) VALUES (?, ?)");
                    insertStmt.setString(1, parts[3]); // autor
                    insertStmt.setString(2, parts[5]); // tresc
                    insertStmt.executeUpdate();

                    Statement countStmt = con.createStatement();
                    ResultSet result = countStmt.executeQuery("SELECT COUNT(*) FROM tablica");
                    result.next();
                    int count = result.getInt(1);

                    if (count > 10) {
                        Statement deleteStmt = con.createStatement();
                        deleteStmt.executeUpdate("DELETE FROM tablica WHERE ID IN (SELECT ID FROM (SELECT ID FROM tablica ORDER BY ID ASC LIMIT 1) AS t)");
                    }

                    odpowiedz = "status:OK";
                    System.out.println("Dodano wpis od użytkownika: " + parts[3]);
                } catch (Exception e) {
                    e.printStackTrace();
                    odpowiedz = "status:NO";
                }

                chatOut.println(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}