import java.io.IOException;
import java.nio.ByteBuffer;

public class CommandServer {
    
    private int commandType; // 0: Execute response
                             // 1: Login or Register response
                             // 2: Info response

    private int responseStatus; // 0: No error
                                // 1: Error

    // Null when commandtype 0                           
    private String errorType; 
    
    // Null when commandtype 1   
    private String fileName;

    // Null when commandtype 1   
    private byte[] responseBytes;


    public CommandServer(int commandType, int responseStatus,String error, String fileName, byte[] responseBytes){
        this.commandType = commandType;
        this.responseStatus = responseStatus;

        if(responseStatus == 1){
            this.errorType = error;
        }else{
            this.errorType = null;
        }

        if(fileName!=null){
            this.fileName = fileName;
        }else{
            this.fileName = null;
        }

        if(responseBytes!=null){
            this.responseBytes = responseBytes.clone();
        }else{
            this.responseBytes = null;
        }
    }

    public int getCommandType() {
        return commandType;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getFileName() {
        return fileName;
    }
    //----------------------------------------------

    public byte[] serialize() throws IOException {
        int size = 0;
        byte[] data = null;

        if(getCommandType()!=0){
            size += 8;
            
            ByteBuffer buffer = ByteBuffer.allocate(size);
            
            // Writing command type
            buffer.putInt(getCommandType());
            
            // Writing response
            buffer.putInt(getResponseStatus());

            // Returning the byte[]
            buffer.rewind();
            data = buffer.array();
        
        } else {
            // Calculating the size
            if(getResponseStatus()==1){
                size += 16 + getFileName().length() +
                             getErrorType().length();
            }else{
                size += 16 + getResponseBytes().length+ 
                             getFileName().length();
            }
            
            ByteBuffer buffer = ByteBuffer.allocate(size);           

            // Writing command type
            buffer.putInt(getCommandType());

            // Writing response
            buffer.putInt(getResponseStatus());

            if(getResponseStatus()==1){
                buffer.putInt(getErrorType().length());
                buffer.put(getErrorType().getBytes());
                
                // Writing file name
                buffer.putInt(getFileName().getBytes().length);
                buffer.put(getFileName().getBytes());
            } else{
                // Writing file name
                buffer.putInt(getFileName().getBytes().length);
                buffer.put(getFileName().getBytes());
                
                // Writing response bytes
                buffer.putInt(getResponseBytes().length);
                buffer.put(getResponseBytes());
            }

            // Returning the byte[]
            buffer.rewind();
            data = buffer.array();
        }

        return data;
    }

    public static CommandServer deserialize(byte[] info) throws IOException {
        
        // Response status
        int responseStatus = 0;

        // File name
        String fileName = null;
        byte[] fileNameBytes = null;

        // Error type
        String error = null;
        byte[] errorBytes = null;

        // Response bytes
        byte[] responseBytes = null;
        
        ByteBuffer buffer = ByteBuffer.wrap(info);
        
        // Getting command type
        int commandType = buffer.getInt();
        
        if(commandType!=0){
    
            // Getting response
            responseStatus = buffer.getInt();
            
        } else{
            // Getting response
            responseStatus = buffer.getInt();
            
            if(responseStatus==1){
                // Getting error type
                int errorLength = buffer.getInt();
                errorBytes = new byte[errorLength];
                buffer.get(errorBytes);
                error = new String(errorBytes);
            
                // Getting file name
                int fileNameLength = buffer.getInt();
                fileNameBytes = new byte[fileNameLength];
                buffer.get(fileNameBytes);
                fileName = new String(fileNameBytes);
            } 
            else{

                // Getting file name
                int fileNameLength = buffer.getInt();
                fileNameBytes = new byte[fileNameLength];
                buffer.get(fileNameBytes);
                fileName = new String(fileNameBytes);
    
                // Getting task bytes
                int responseBytesLength = buffer.getInt();
                responseBytes = new byte[responseBytesLength];
                buffer.get(responseBytes);
            }
        }

        return new CommandServer(commandType,responseStatus,error,fileName,responseBytes);
    }
}
