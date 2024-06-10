import java.io.*;
import java.net.*;

public class HandlerServer implements Runnable{

    private Socket clientSocket;
    
    private ServerData serverData;
    
    public HandlerServer (ServerData serverData, Socket clientSocket){
        this.serverData = serverData;
        this.clientSocket = clientSocket;
    }
    
    public Socket getClientSocket() {
        return clientSocket;
    }
    
    public ServerData getServerData() {
        return serverData;
    }
    
    private boolean verificaLogin(byte[] data) throws IOException{

        CommandClient command = CommandClient.deserialize(data);
        
        return getServerData().loginUser(command.getUsername(),command.getPassword());
    }

    private boolean verificaRegisto(byte[] data) throws IOException {

        CommandClient command = CommandClient.deserialize(data);
        
        return getServerData().registerUser(command.getUsername(),command.getPassword());
    }

    @Override
    public void run() {

        System.out.println("\u001B[32m A Client with the socket: " +getClientSocket()+ " connected with server! \u001B[0m\n\n ");

        try {

            Demultiplexer m = new Demultiplexer(new TaggedConnection(getClientSocket()));
            m.start();

            // Threads that receives requests
            Thread[] threads = {

                // Thread Executar
                new Thread(() -> {
                    try  {
                        // Receives execute requests and archives them in the server data to be execute by the thread pool
                        while(true){
                            // Getting the data
                            byte[] data = m.receive(0);
                            System.out.println("Received execution request!");

                            // Getting the information
                            CommandClient command = CommandClient.deserialize(data);

                            // Executing the task
                            int i = getServerData().addTask(m,command);

                            // In case of error
                            if(i==-1){
                                
                                // Task byte array is null
                                if(command.getTaskBytes()==null){
                                    String error = "Nothing to execute!";
                                    CommandServer res = new CommandServer(0, 1, error, command.getTaskName(),null);
    
                                    Frame frame = new Frame(0,res.serialize());

                                    m.send(frame);
                                } 
                                // Memory exceeds memory limit
                                else {
                                    String error = "Excedeed memory limit!";
                                    CommandServer res = new CommandServer(0, 1, error, command.getTaskName(),null);

                                    Frame frame = new Frame(0,res.serialize());

                                    m.send(frame);
                                }
                            }
                            
                        }
                    }  catch (Exception ignored) {}
                }),
        
                // Thread Login
                new Thread(() -> {
                    try  {
                            
                        while(true){

                            // Getting the data
                            byte[] data = m.receive(1);
                            System.out.println("Received login request!");

                            boolean veredito = verificaLogin(data);

                            if(veredito){ 

                                CommandServer command = new CommandServer(1, 0, null,null, null);
                                byte[] info = command.serialize();
                                //System.out.println("Log in sucess!");

                                Frame frame = new Frame(1, info);
                                m.send(frame);
                            } else{

                                String erro = "Login fail!";
                                CommandServer command = new CommandServer(1, 1, erro,null, null);
                                byte[] info = command.serialize();

                                Frame frame = new Frame(1, info);
                                m.send(frame);
                            }
                        }
                    }  catch (Exception ignored) {}
                }),
        
                // Thread Registo
                new Thread(() -> {
                    try  {
                        while(true){

                            // Getting the data
                            byte[] data = m.receive(2);
                            System.out.println("Received register request!");

                            boolean veredito = verificaRegisto(data);

                            if(veredito){ 
                                CommandServer command = new CommandServer(1, 0, null,null, null);
                                byte[] info = command.serialize();
                                //System.out.println("Register sucess!");

                                Frame frame = new Frame(1, info);
                                m.send(frame);
                            }
                            else{
                                String erro = "Registration fail!";
                                CommandServer command = new CommandServer(1, 1, erro,null, null);
                                byte[] info = command.serialize();

                                Frame frame = new Frame(1, info);
                                m.send(frame);
                            }
                        }
                    }  catch (Exception ignored) {}
                }),

                // Thread Information
                new Thread(() -> {
                    try  {
                        while(true){

                            // Getting the data
                            m.receive(3);
                            System.out.println("Received info request!");

                            int freeMem = getServerData().getFreeMemory();

                            CommandServer command = new CommandServer(2, freeMem, null,null, null);
                            byte[] info = command.serialize();

                            Frame frame = new Frame(2, info);
                            m.send(frame);     
                        }
                    }  catch (Exception ignored) {}
                })
            };
        
            for (Thread t: threads) t.start();
            for (Thread t: threads) t.join();
            

        } catch (Exception e) {
                System.err.println("Details: " + e.getMessage() + "\n");
                e.printStackTrace();
        }
    }

}
