

public class Frame {
        
    public final int tag;      // 0 : execute       
                               // 1 : login
                               // 2 : register

    public final byte[] data;

    public Frame(int tag, byte[] data) { 
        this.tag = tag; 
        this.data = data; 
    }
}
