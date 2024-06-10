
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<E> {
 
    private final Lock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();
    // BlockingQueue using LinkedList structure
    // with a constraint on capacity
    private List<E> queue = new LinkedList<E>();
 
    // limit variable to define capacity
    private int limit = 100;
 
    // constructor of BlockingQueue
    public MyBlockingQueue(int limit) { this.limit = limit; }
 
    public void enqueue(E item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == limit) {
                notFull.await();
            }
            queue.add(item);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public E dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == 0) {
                notEmpty.await();
            }
            E item = queue.remove(0);
            notFull.signalAll();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(E e) throws InterruptedException {
        lock.lock();
        try {
            if (queue.size() == limit) {
                return false;
            }
            queue.add(e);
            notEmpty.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == 0) {
                notEmpty.await();
            }
            E item = queue.remove(0);
            notFull.signalAll();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

}
