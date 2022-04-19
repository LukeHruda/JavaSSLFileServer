import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocketFactory;

public class Server {
    public static void main(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.keyStore","group.18");
        System.setProperty("javax.net.ssl.keyStorePassword","group18");
        ServerSocket serverSocket = ((SSLServerSocketFactory)SSLServerSocketFactory.getDefault()).createServerSocket(4444);

        System.out.println("Server active");
        while (true) new Serverthread(serverSocket.accept()).start();
    }
}
