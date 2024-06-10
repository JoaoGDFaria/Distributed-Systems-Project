import sd23.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class ServerData {
    
    private static int memoryLimit;

    private int memmoryInUse;
    
    private int numConnections;

    // Taks info
    private List<QueueInfo> queue;
    
    // Login & Register info
    private Map<String,String> userInfo;  // Key : Username
                                          // Value : PassWord

    // Thread Utilities
    private ReentrantLock lock;

    private Condition condExecute;
    private Condition condMem;

    // Constructors

    public ServerData(String memoryLimit) {
        
        ServerData.memoryLimit = Integer.parseInt(memoryLimit);
        this.memmoryInUse = 0;
        this.numConnections = 0;
        
        this.queue = new ArrayList<>();

        this.lock = new ReentrantLock();
        this.condExecute = lock.newCondition();
        this.condMem = lock.newCondition();
        
        this.userInfo = new HashMap<String,String>();
    }


    // Getters
    public static int getMemoryLimit() {
        return memoryLimit;
    }

    public int getMemmoryInUse() {
        lock.lock();
        try{
            return memmoryInUse;
        } finally {
            lock.unlock();
        }
    }
    
    public int getNumConnections() {
        lock.lock();
        try{
            return numConnections;
        } finally {
            lock.unlock();
        }
    }

    public List<QueueInfo> getQueue() {
        return queue;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Map<String,String> getUserInfo() {
        return userInfo;
    }
    
    // Methods //////////////////////////////////////////////////////////////////////////////////////
    public void decrementMem(int memToDelete){
        lock.lock();
        try{
            this.memmoryInUse = this.memmoryInUse - memToDelete;
            condMem.signalAll();    
        }
        finally{
            lock.unlock();    
        }
    }

    public void incrementMem(int memToAdd){
        lock.lock();
        try{
            this.memmoryInUse = this.memmoryInUse + memToAdd;   
        }
        finally{
            lock.unlock();    
        }
    }

    public void newConnection(Socket client) {
        lock.lock();
        try{
            //getFinishedTasks().put(client, new ArrayList<>());
            this.numConnections++;
        }
        finally{
            lock.unlock();    
        }
    }

    public boolean registerUser(String userName, String passWord) {
        lock.lock();
        try {
            if(!this.userInfo.containsKey(userName)){
               //System.out.println("INSERIU" + userName + " " + passWord);
               this.userInfo.put(userName, passWord);
               return true;
            }    
            else{
                return false;
            }       
        } finally {
            lock.unlock();
        }

    }

    public boolean loginUser(String username, String passWord){
        lock.lock();
        try {
            if(this.userInfo.containsKey(username) && this.userInfo.get(username).equals(passWord)){
                return true;
            } 
            else{
                return false;
            } 
                
        } finally {
            lock.unlock();
        }
    }

    public void printInfo() {
        lock.lock();
        try {
            System.out.println("Free memory: " +(getMemoryLimit()-getMemmoryInUse())+ "Bytes\n\n" );
            System.out.println("Queue:  "+ getQueue() +"\n\n");
        } finally {
            lock.unlock();
        }
    }

    public int addTask(Demultiplexer m,CommandClient command){
        
        if(command.getMemoryNeeded()>memoryLimit || command.getTaskBytes()==null) return -1;
        else{
            getLock().lock();
            try{
                QueueInfo info = new QueueInfo(command,m);
                getQueue().add(info);
                condExecute.signal();
                return 0;
            } finally{
                getLock().unlock();
            }
        }
    }

    public void executeTask() throws InterruptedException, IOException {
        while (true) {
            
            lock.lock();
            try {
                // Espera enquanto nao existe nenhuma tarefa para executar
                while(getQueue().isEmpty()){ 
                    condExecute.await();
                }    
            } finally {
                lock.unlock();
            }

            // Deciding in which command to execute
            //System.out.println("Vou escolher qual executar");
            QueueInfo commmand = findCommand();
            
            Boolean randomFlag = false;
            byte[] info = null;
            try{
                // Executing
                //System.out.println("Vou executar");
                info = JobFunction.execute(commmand.getInfo().getTaskBytes()); 

            } catch (JobFunctionException e) {
                randomFlag = true;
            }

            // Decrementing the memory
            decrementMem(commmand.getInfo().getMemoryNeeded());

            byte[] message = null;
            if(!randomFlag){
                // Preparing the message
                CommandServer toSend = new CommandServer(0, 0, null, commmand.getInfo().getTaskName(), info);
                message = toSend.serialize();

            } else {
                String error = "Execution failed!";
                // Preparing the message
                CommandServer toSend = new CommandServer(0, 1, error, commmand.getInfo().getTaskName(), null);
                message = toSend.serialize();
            }

            // Sending the frame
            //System.out.println("vou enviar");
            commmand.getClient().send(0,message);
        }
    }

    public QueueInfo findCommand() throws InterruptedException{
        lock.lock();
        try{
            while(true){

                QueueInfo chosenOne = null;

                for(QueueInfo entry : getQueue()){
                    int availableMem = getMemoryLimit() - getMemmoryInUse();
                    entry.increment();
                    if(entry.getCount()>=5){
                        if(entry.getInfo().getMemoryNeeded() <= availableMem){
                            chosenOne = entry;
                            getQueue().remove(entry);
                            incrementMem(chosenOne.getInfo().getMemoryNeeded());
                            return chosenOne;
                        } else{
                            chosenOne = entry;
                            break;
                        }
                    } 
                }    

                if(chosenOne==null){
                    for(QueueInfo entry : getQueue()){
                        int availableMem = getMemoryLimit() - getMemmoryInUse();
                        if(entry.getInfo().getMemoryNeeded() <= availableMem) {
                            chosenOne = entry;
                            getQueue().remove(entry);
                            incrementMem(chosenOne.getInfo().getMemoryNeeded());
                            return chosenOne;
                        }
                    }   
                }
                
                while(true){
                    this.condMem.await();
                    break;
                }

            }
        }
        finally{
            lock.unlock();
        }

    }

    public int getFreeMemory() {
        return getMemoryLimit()-getMemmoryInUse();
    }

}