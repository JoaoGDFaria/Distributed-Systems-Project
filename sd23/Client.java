import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {
 
    public static void main(String[] args) throws IOException {

        Path currentDirectoryPath = Paths.get(System.getProperty("user.dir"));
        Path parentDirectoryPath = currentDirectoryPath.getParent();
        Path result = parentDirectoryPath.resolve("Resultados/" + args[0]);
        Files.createDirectories(result);

        // Estabelece a conecxao com o Servidor
        try (Socket serverSocket = new Socket("localhost",42069)) {
             
            System.out.println("\u001B[32m Client connected to server. \u001B[0m\n");
        
            TaggedConnection middleWare = new TaggedConnection(serverSocket);

            ClientMenus menus = new ClientMenus(new Demultiplexer(middleWare),result);
            menus.getMiddleWare().start();
            menus.initMenu();

        } catch (Exception e) {
            System.out.println("\u001B[31m Client failed to connect to server! \u001B[0m\n");
            System.err.println("Details: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }
}

