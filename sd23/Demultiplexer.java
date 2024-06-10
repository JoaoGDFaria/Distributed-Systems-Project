

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer {
    
    private TaggedConnection conn;
    private ReentrantLock lock;   
    
    private class Entry{
        Condition cond = lock.newCondition();
        Deque<byte[]> queue = new ArrayDeque<>();
        int waiters = 0;
    }
    
    private Map<Integer,Entry> map;
    IOException ioe = null;
    
    public Demultiplexer(TaggedConnection conn) {
        this.conn = conn;
        this.lock = new ReentrantLock();
        this.map = new HashMap<>(); 
    }
    
    private Entry get(int tag){
        Entry entry = map.get(tag);
        if(entry == null){
            entry = new Entry();
            map.put(tag, entry);
        }
        return entry;
    }
    
    public void start() {
        new Thread(() -> {
            try {
                while(true) {
                    Frame f = this.conn.receive();
                    
                    int tag = f.tag;
                    byte[] bytes = f.data;
                    
                    lock.lock();
                    try{
                        Entry entry = get(tag);
                        entry.queue.add(bytes);
                        entry.cond.signal();
                    } finally {
                        lock.unlock();
                    }
                    
                } 
            } catch (IOException e) {
                lock.lock();
                try{
                    ioe = e;
                    for(Entry entry : map.values()) entry.cond.signalAll();
                
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }
    
    public void send(Frame frame) throws IOException {
        conn.send(frame);
    }
    
    public void send(int tag, byte[] data) throws IOException {
        conn.send(tag,data);
    }
    
    public byte[] receive(int tag) {
        
        lock.lock();
        try{
            
            Entry entry = get(tag);
            
            entry.waiters++;
            
            while(entry.queue.isEmpty()){ 
                try {
                    entry.cond.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            entry.waiters--;
            
            byte[] info = entry.queue.poll();
            
            if(entry.waiters == 0 && entry.queue.isEmpty()) 
            map.remove(tag);
            
            return info;
            
        } finally {
            lock.unlock();
        }
        
    }
    
    public void close() throws IOException {
        conn.close();
    }
}
