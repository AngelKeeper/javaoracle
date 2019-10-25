package com.denisfedotov.javaoracle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class App {
    private static Connection connection;

    public static void main(String args[]) {

        String dbUser = System.getenv("DBUSER");
        String dbPassword = System.getenv("DBPASSWORD");
        String dbUrl = System.getenv("DBURL");

        if (dbUser == null) {
            dbUser = "mad_ref_data";
        }

        if (dbPassword == null) {
            dbPassword = "mad";
        }

        if (dbUrl == null) {
            dbUrl = "jdbc:oracle:thin:@localhost:1521:xe";
        }

        try  {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            if (conn ==null) {
                System.out.println("Failed to make connection!");
                return;
            }
            System.out.println("DB connected");
            connection = conn;

            HttpServer server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);

            server.createContext("/", new MainHttpHandler());
            server.createContext("/actuator/health", new HealthCheckHttpHandler());

            server.setExecutor(null);
            server.start();

        } catch (SQLException e) {
            System.out.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MainHttpHandler implements HttpHandler {

        public void handle(HttpExchange httpExchange) throws IOException {
            byte[] bytes = "Java Oracle".getBytes();
            httpExchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class HealthCheckHttpHandler implements HttpHandler {

        public void handle(HttpExchange httpExchange) throws IOException {
            if (connection == null)
            {
                String noConnection = "Java Oracle HealthCheckHttpHandler No connection";
                sendError(httpExchange, noConnection);
                return;
            }

            try {
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select 'hello' from dual");
                rs.next();
                String result = rs.getString(1);
                httpExchange.getResponseHeaders().add("encoding", "UTF-8");
                httpExchange.sendResponseHeaders(200, result.length());
                httpExchange.getResponseBody().write(result.getBytes());
                httpExchange.close();
            } catch (SQLException e) {
                sendError(httpExchange, e.getMessage());
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

        private static void sendError(HttpExchange httpExchange, String message) throws IOException {
            httpExchange.getResponseHeaders().add("encoding", "UTF-8");
            httpExchange.sendResponseHeaders(500, message.length());
            httpExchange.getResponseBody().write(message.getBytes());
            httpExchange.close();
        }
    }
}
