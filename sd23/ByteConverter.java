import java.io.*;

public class ByteConverter {

    public static byte[] convertFileToBytes(File file) throws IOException {
        
        FileInputStream inputFile = new FileInputStream(file);
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; 

        int bytesRead;
        while ((bytesRead = inputFile.read(buffer)) != -1) {
            outputBytes.write(buffer, 0, bytesRead);
        }

        inputFile.close();
        outputBytes.close();

        return outputBytes.toByteArray();
    }

}