import java.io.*;
import java.net.*;
import java.sql.*;

public class Register {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        ServerSocket registerSocket = new ServerSocket(2138);
        System.out.println("Serwer rejestracji uruchomiony na porcie 2138");

        while (true) {
            try {
                Socket clientSocket = registerSocket.accept();
                PrintWriter registerOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader registerIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String message = registerIn.readLine();
                System.out.println("Otrzymano żądanie rejestracji: " + message);
                String[] odebrane = message.split("#|:");
                String odpowiedz;

                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/ts", "root", "karol1")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    PreparedStatement checkStmt = con.prepareStatement("select * from users where login=?");
                    checkStmt.setString(1, odebrane[3]);
                    ResultSet result = checkStmt.executeQuery();

                    if (!result.next()) {
                        PreparedStatement insertStmt = con.prepareStatement("INSERT INTO users (login,haslo) VALUES (?,?)");
                        insertStmt.setString(1, odebrane[3]);
                        insertStmt.setString(2, odebrane[5]);
                        insertStmt.executeUpdate();
                        odpowiedz = "status:OK";
                        System.out.println("Zarejestrowano nowego użytkownika: " + odebrane[3]);
                    } else {
                        odpowiedz = "status:NO";
                        System.out.println("Próba rejestracji istniejącego użytkownika: " + odebrane[3]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    odpowiedz = "status:NO";
                }

                registerOut.println(odpowiedz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}