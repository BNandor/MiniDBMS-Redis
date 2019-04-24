import comm.Server;
import comm.Worker;

public class Main {
    public static void main(String[] args) {
        Worker worker = new Worker();
        Server server = new Server(1695, worker);
        server.start();
    }
}
