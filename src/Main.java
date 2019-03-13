import com.sun.corba.se.spi.orbutil.threadpool.Work;
import comm.Client;
import comm.Server;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import struct.*;

import java.io.*;


public class Main {

    public static void main(String[] args) throws IOException {

        Worker worker = new Worker();
        Server server = new Server(1695,worker);
        server.start();

    }
}

