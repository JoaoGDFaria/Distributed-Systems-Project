import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class CommandClient {

    private int commandType; // 0: Execute task

                             // 1: Login

                             // 2: Register 

    private String username; //  pode ser null
    private String password; //  pode ser null

    private String taskName;  // pode ser null
    private int memoryNeeded; // pode ser null
    private byte[] taskBytes; //  pode ser null 

    public CommandClient(int commandType, String username, String password,String taskName, int memoryNeeded, byte[] taskBytes){
        this.commandType = commandType;

        this.username = username;
        this.password = password;

        this.taskName = taskName;
        this.memoryNeeded = memoryNeeded;
        if(taskBytes!=null){
            this.taskBytes = taskBytes.clone();
        }
    }
    
    public int getCommandType() {
        return commandType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTaskName() {
        return taskName;
    }

    public int getMemoryNeeded() {
        return memoryNeeded;
    }

    public byte[] getTaskBytes() {
        return taskBytes;
    }

    public byte[] serialize() throws IOException {
        int size = 4;
        byte[] data = null;

        if(getCommandType()==3){
            ByteBuffer buffer = ByteBuffer.allocate(size);

            // Writing command type
            buffer.putInt(getCommandType());

            // Returning the byte[]
            buffer.rewind();
            data = buffer.array();
        }else if(getCommandType()!=0){
            size += 8 + getUsername().getBytes().length +
                        getPassword().getBytes().length;
            
            ByteBuffer buffer = ByteBuffer.allocate(size);
            
            // Writing command type
            buffer.putInt(getCommandType());
            
            // Writing username
            byte[] username = getUsername().getBytes();
            buffer.putInt(username.length);
            buffer.put(username);

            // Writing password
            byte[] password = getPassword().getBytes();
            buffer.putInt(password.length);
            buffer.put(password);

            // Returning the byte[]
            buffer.rewind();
            data = buffer.array();
        
        } else {
            size += 12 + getTaskName().getBytes().length +
                        getTaskBytes().length;
            
            ByteBuffer buffer = ByteBuffer.allocate(size);           

            // Writing command type
            buffer.putInt(getCommandType());

            // Writing task name
            byte[] taskName = getTaskName().getBytes();
            buffer.putInt(taskName.length);
            buffer.put(taskName);

            // Writing memory nedded 
            buffer.putInt(getMemoryNeeded());

            // Writing task bytes
            buffer.putInt(getTaskBytes().length);
            buffer.put(getTaskBytes());

            // Returning the byte[]
            buffer.rewind();
            data = buffer.array();
        }

        return data;
    }

    public static CommandClient deserialize(byte[] info) throws IOException {
        
        // For login and register
        String username = null;
        String password = null;
        
        // For executing and task
        String taskName = null;
        int memoryNeeded = 0;
        byte[] taskBytes = null;
        
        ByteBuffer buffer = ByteBuffer.wrap(info);
        
        // Getting command type
        int commandType = buffer.getInt();
        
        if(commandType!=0 && commandType!=3){
    
            // Getting username
            int usernameLength = buffer.getInt();
            byte[] usernameBytes = new byte[usernameLength];
            buffer.get(usernameBytes);
            username = new String(usernameBytes, StandardCharsets.UTF_8);

            // Getting password
            int passwordLength = buffer.getInt();
            byte[] passwordBytes = new byte[passwordLength];
            buffer.get(passwordBytes);
            password = new String(passwordBytes, StandardCharsets.UTF_8);
            
        } else{
            
            // Getting task name
            int taskNameLength = buffer.getInt();
            byte[] taskNameBytes = new byte[taskNameLength];
            buffer.get(taskNameBytes);
            taskName = new String(taskNameBytes, StandardCharsets.UTF_8);

            // Getting memory needed
            memoryNeeded = buffer.getInt();

            // Getting task bytes
            int taskBytesLength = buffer.getInt();
            taskBytes = new byte[taskBytesLength];
            buffer.get(taskBytes);
            
        }

        return new CommandClient(commandType,username,password,taskName,memoryNeeded,taskBytes);
    }

}
