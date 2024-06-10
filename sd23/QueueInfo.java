

public class QueueInfo {

    private int count;
    private CommandClient info;
    private Demultiplexer client;

    public int getCount() {
        return count;
    }

    public CommandClient getInfo() {
        return info;
    }

    public Demultiplexer getClient() {
        return client;
    }

    public QueueInfo(CommandClient info, Demultiplexer client){
        this.count = 0;
        this.info = info;
        this.client = client;
    }

    public void increment(){
        this.count++;
    }
}
