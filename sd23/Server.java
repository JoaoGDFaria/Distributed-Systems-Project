import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        
        ServerData serverData = new ServerData(args[0]);

        try {
            try (ServerSocket serverSocket = new ServerSocket(42069)) {
                Thread executingThread = new Thread( () -> {

                    int numberOfThreads = 5;
                    MyThreadPool threadPool = new MyThreadPool(numberOfThreads, 100);

                    for(int i=0; i<numberOfThreads; i++) {
                        try {
                            threadPool.execute( () -> {try {
                                serverData.executeTask();
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }});
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                executingThread.start();

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    serverData.newConnection(clientSocket);
                    Thread t = new Thread(new HandlerServer(serverData,clientSocket));
                    t.start();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
