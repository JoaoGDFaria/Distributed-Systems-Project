

public class PoolThreadRunnable implements Runnable{
    private Thread                thread    = null;
    private MyBlockingQueue<Runnable> taskQueue = null;
    private boolean               isStopped = false;

    public PoolThreadRunnable(MyBlockingQueue<Runnable> queue){
        taskQueue = queue;
    }

    public void run(){
        this.thread = Thread.currentThread();
        try{
                Runnable runnable = (Runnable) taskQueue.take();
                runnable.run();
        } catch(Exception e){}
    }
}
