import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientMenus {
    
    private static String cleanTerminal = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";

    // User info
    private String currentUser;
    private final String path;

    // Thread utilities
    private ReentrantReadWriteLock lock;

    // Results info
    private Map<String,Map<String,ResultInfo>> userTasks;
    
    // Midleware
    private Demultiplexer middleWare;

    private Scanner scanner;
    
    // Constructors //////////////////////////////////////////////////////////////////////////

    public ClientMenus(Demultiplexer middleWare, Path path){
      this.currentUser = null;
      this.path = path.toString();  

      this.userTasks = new HashMap<>();

      this.middleWare = middleWare;   
      this.scanner = new Scanner(System.in);

      this.lock = new ReentrantReadWriteLock();
    }

    // Getters //////////////////////////////////////////////////////////////////////////////

    public static String getCleanTerminal() {
        return cleanTerminal;
    }

    public String getCurrentUser() {
        lock.readLock().lock();
        try{
            return this.currentUser;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Demultiplexer getMiddleWare() {
        return middleWare;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public Map<String,Map<String,ResultInfo>> getTasksMap(){
        lock.readLock().lock();
        try{
            return this.userTasks;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public Map<String,ResultInfo> getUserTasks() {
        Map<String,Map<String,ResultInfo>> aux = getTasksMap();

        lock.readLock().lock();
        try{
            return aux.get(getCurrentUser());
        } finally {
            lock.readLock().unlock();
        }    
    }

    public void initUser(){
        Map<String,Map<String,ResultInfo>> aux = getTasksMap();

        if(!aux.containsKey(getCurrentUser())){
            lock.writeLock().lock();
            try{
                aux.put(getCurrentUser(),new HashMap<>());
            } finally {
                lock.writeLock().unlock();
            }
        } 
    }

    public String getPath() {
        return path;
    }
    // Setters //////////////////////////////////////////////////////////////////////////////
    
    public void setCurrentUser(String currentUser) {
        lock.writeLock().lock();
        try{
            this.currentUser = currentUser;
        } finally {
            lock.writeLock().unlock();
        }    
    }

    private void setInMapTrue(String fileName, String error) {
        Map<String,ResultInfo> data = getUserTasks();
        
        lock.writeLock().lock();
        try{
            ResultInfo info = new ResultInfo();
            info.setError(error);
            info.setTrue();
            data.put(fileName, info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void setInMapFalse(String fileName) {
        Map<String,ResultInfo> data = getUserTasks();
        
        lock.writeLock().lock();
        try{
            ResultInfo info = new ResultInfo();
            data.put(fileName, info);
        } finally {
            lock.writeLock().unlock();
        }    
    }

    // Methods //////////////////////////////////////////////////////////////////////////////

    public void initMenu() throws IOException, InterruptedException {

        Thread receivingResponses = new Thread(() -> {

            try {
                while(true){

                    // Receiving info
                    byte[] receiveInfo = getMiddleWare().receive(0);
                
                    // Unvailing info
                    CommandServer commandInfo = CommandServer.deserialize(receiveInfo);  
    
                    String fileName = commandInfo.getFileName() + ".7z";
                    Path caminho = Paths.get(path + "/" + fileName);
                    
                    if(commandInfo.getResponseStatus()==1){
                        setInMapTrue(commandInfo.getFileName(),commandInfo.getErrorType());
                    }
                    else{
                        setInMapTrue(commandInfo.getFileName(), null);
                        Files.write(caminho, commandInfo.getResponseBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        receivingResponses.start();

        mainMenu();

    }
    // Working
    public void mainMenu() throws IOException, InterruptedException{
        
        System.out.println(getCleanTerminal());
        System.out.println(" ------------- WELCOME -------------\n");
        System.out.println("Chose an option:\n");
        System.out.println("1 - Login");
        System.out.println("2 - Register");
        System.out.println("3 - Exit\n");
        System.out.print("    -> ");
        
        int option = 0;

        try {
            option = Integer.parseInt(getScanner().nextLine());
        } catch(NumberFormatException e){           
            System.out.println("\n\nInvalid input. Please try again\n");
            Thread.sleep(2000);
            mainMenu();
        }
        
        if(option == 1) loginMenu();
        else if(option == 2) registerMenu();
        else if(option == 3) {
            System.out.println("Exiting ....");
            Thread.sleep(2000);
            getMiddleWare().close();
            getScanner().close();
            System.exit(0); 
        }
        else {
            System.out.println("\n\nInvalid input. Please try again\n");
            Thread.sleep(2000);
            mainMenu();
        }

    }

    public void registerMenu() throws InterruptedException, IOException{
        System.out.println(getCleanTerminal());
        System.out.println(" ------------------------------------\n");

        System.out.print("Username: ");
        String username = getScanner().nextLine();
        
        System.out.print("Password: ");
        String noOneWillUseThisString = getScanner().nextLine();
        System.out.print("Confirm Password: ");
        String password = getScanner().nextLine();
        
        if(!noOneWillUseThisString.equals(password)){
            System.out.println("\n\nPasswords do not match\n");
            System.out.println("Try again");
            Thread.sleep(2000);
            mainMenu();
        }

        // Preparing registation request
        CommandClient commandData = new CommandClient(2, username, password,null,0, null);
        byte[] data = commandData.serialize();
            
        // Sending registation request
        Frame frame = new Frame(2, data);
        getMiddleWare().send(frame);
            
        // Receiving info
        byte[] receiveInfo = getMiddleWare().receive(1);
            
        // Unvailing info
        CommandServer commandInfo = CommandServer.deserialize(receiveInfo);  

        if(commandInfo.getResponseStatus()==0){
            System.out.println("\n\nRegistation sucessfull");
            System.out.println("\nPlease login");
            Thread.sleep(2000);
        }
        else {
            System.out.println("\n\nUsername already exists!\n");
            System.out.println("Try again");
            Thread.sleep(2000);
        }
        
        mainMenu();
            
    }

    public void loginMenu() throws IOException, InterruptedException{
        System.out.println(getCleanTerminal());
        System.out.println(" ------------------------------------\n");

        System.out.print("Username: ");
        String username = getScanner().nextLine();

        System.out.print("Password: ");
        String password = getScanner().nextLine();

        // Preparing login request
        CommandClient commandData = new CommandClient(1, username, password,null,0, null);
        byte[] data = commandData.serialize();
            
        // Sending login request
        Frame frame = new Frame(1, data);
        getMiddleWare().send(frame);
            
        // Receiving info
        byte[] receiveInfo = getMiddleWare().receive(1);
            
        // Unvailing info
        CommandServer commandInfo = CommandServer.deserialize(receiveInfo);  

        if(commandInfo.getResponseStatus()==0){
            System.out.println("\n\nLogin efetuado com successo");
            Thread.sleep(2000);
            setCurrentUser(username);
            initUser();
            interfaceMenu();
        }
        else{
            System.out.println("\n\nInvalid username and/or password\n");
            System.out.println("Try again");
            Thread.sleep(2000);
            mainMenu();
        }

    }

    public void interfaceMenu() throws IOException, InterruptedException{
        
        System.out.println(getCleanTerminal());
        System.out.println(" ------------- Functionalities -------------\n");
        System.out.println("Chose an option:\n");
        System.out.println("1 - Execute");
        System.out.println("2 - Results");
        System.out.println("3 - Information");
        System.out.println("4 - Logout\n");
        System.out.print("    -> ");

        int option = 0;

        try {
           option = Integer.parseInt(getScanner().nextLine());
        } catch(NumberFormatException e){           
            System.out.println("\n\nInvalid input. Please try again\n");
            Thread.sleep(2000);
            interfaceMenu();
        }

        if(option == 1) executeMenu();
        else if(option == 2) resultsMenu();
        else if(option == 3) infoMenu();
        else if(option == 4) {
            System.out.println("Loging out ....");
            Thread.sleep(2000);
            setCurrentUser(null);
            mainMenu();
        }
        else {
            System.out.println("\n\nInvalid input. Please try again\n");
            Thread.sleep(2000);
            interfaceMenu();
        }

    }

    public void executeMenu() throws IOException, InterruptedException{
        System.out.println(getCleanTerminal());
        System.out.println(" ------------------------------------\n");
        
        System.out.print("Insert executable file path: ");
        String filePath = getScanner().nextLine();
        
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File does not exists!");
            System.out.println("Please try again");
            Thread.sleep(2000);
            interfaceMenu();
        } 
        String fileName = file.getName();

        System.out.print("Insert memory nedeed to execute the file: ");
        int memoryNedeed = Integer.parseInt(getScanner().nextLine());
        
        // Thread to handle sending request to server so the user can perform other actions
        Thread sendExecution = new Thread(() -> {
            
            try {
                // Preparing execution request
                CommandClient commandData = new CommandClient(0, null, null,fileName,memoryNedeed, ByteConverter.convertFileToBytes(file));
                byte[] data = commandData.serialize();

                // Inserting the file in the map
                setInMapFalse(fileName);
                
                // Sending execution request
                Frame frame = new Frame(0, data);
                getMiddleWare().send(frame);
                //System.out.println("Enviei pedido");
                
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        sendExecution.start();

        interfaceMenu();
    }

    public void resultsMenu() throws IOException, InterruptedException{
        System.out.println(getCleanTerminal());
        System.out.println(" -------------------RESULTADOS-------------------\n");
        System.out.println();

        Map<String,ResultInfo> data = getUserTasks();

        int i = 0;
        for(Map.Entry<String,ResultInfo> entry : data.entrySet()){
            ResultInfo info = entry.getValue();
            if(info.getBool()){
                i++;
                if(info.getError()==null){
                    System.out.println("   " + entry.getKey() + "-> SUCESSO!");
                } else{
                    System.out.println("   " + entry.getKey() + "-> " + info.getError());
                }
            }
        }
        if(i==0) System.out.println("   None\n\n");

        System.out.print("\n\nPress enter to go back!");
        getScanner().nextLine();
        interfaceMenu();
    }

    private void infoMenu() throws IOException, InterruptedException {
        System.out.println(getCleanTerminal());
        System.out.println(" ------------------------------------\n");

        System.out.println("Waiting for results ...");

        // Preparing login request
        CommandClient commandData = new CommandClient(3, null, null,null,0, null);
        byte[] data = commandData.serialize();
            
        // Sending login request
        Frame frame = new Frame(3, data);
        getMiddleWare().send(frame);
            
        // Receiving info
        byte[] receiveInfo = getMiddleWare().receive(2);
            
        // Unvailing info
        CommandServer commandInfo = CommandServer.deserialize(receiveInfo);  

        int freeMemomy = commandInfo.getResponseStatus();
        System.out.println(getCleanTerminal());
        System.out.println("Free memory to use: " + freeMemomy);

        System.out.println("\nPending tasks: \n");
        int i = 1;
        for(Map.Entry<String,ResultInfo> entry : getUserTasks().entrySet()){
            if(!entry.getValue().getBool()){
                System.out.println("   " + i + " -> " + entry.getKey());
                i++;
            }
        }
        if(i==1) System.out.println("  None\n\n");

        System.out.print("\n\nPress enter to go back!");
        getScanner().nextLine();
        interfaceMenu();
    }

}

