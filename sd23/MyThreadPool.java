import java.util.ArrayList;
import java.util.List;


public class MyThreadPool {
    
    private MyBlockingQueue<Runnable> taskQueue = null;
    private List<PoolThreadRunnable> runnables = new ArrayList<>();
    private boolean isStopped = false;

    public MyThreadPool(int noOfThreads, int maxNoOfTasks){
        taskQueue = new MyBlockingQueue<Runnable>(maxNoOfTasks);

        for(int i=0; i<noOfThreads; i++){
            PoolThreadRunnable poolThreadRunnable =
                    new PoolThreadRunnable(taskQueue);

            runnables.add(poolThreadRunnable);
        }
        for(PoolThreadRunnable runnable : runnables){
            new Thread(runnable).start();
        }
    }

    public void execute(Runnable task) throws Exception{
        this.taskQueue.add(task);
    }

}
