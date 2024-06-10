import java.io.IOException;

public class Task implements Runnable{

    private ServerData data;

    public Task(ServerData data){
        this.data = data;
    }

    public ServerData getData() {
        return data;
    }

    @Override
    public void run() {
        
        try {
            getData().executeTask();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    

}
