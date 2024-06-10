import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TaggedConnection implements AutoCloseable {
    
    private final Socket s;

    // Canudo de escrita
    private final DataOutputStream out;
    private ReentrantLock outLock;

    // Canudo de leitura
    private final DataInputStream in;
    private ReentrantLock inLock;

    public TaggedConnection(Socket socket) throws IOException { 
        this.s = socket;

        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.outLock = new ReentrantLock();

        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.inLock = new ReentrantLock();
    }
    
    public void send(Frame frame) throws IOException { 
        send(frame.tag, frame.data);
    }
    
    public void send(int tag, byte[] data) throws IOException { 
        this.outLock.lock();
        try {
          out.writeInt(tag);
          out.writeInt(data.length);
          out.write(data);
          out.flush(); 

        } finally {
          this.outLock.unlock();
        }

    }
    
    public Frame receive() throws IOException {  
        byte[] data = null;
        int tag = 0;

        this.inLock.lock();
        try {
          tag = this.in.readInt(); 
          data = new byte[in.readInt()];
          this.in.readFully(data);

        } finally {
          this.inLock.unlock();
        }

        return new Frame(tag, data);  
    }
    
    public void close() throws IOException {
        out.close();
        in.close();
        s.close();
    }

}
