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
        Client.getClient().write("Use Tmp"+"\n");

//        for(int i=0;i<10000;i++){
//            Client.getClient().write("insert into one values ( "+i+" , "+" test )"+"\n");
//            System.out.println(Client.getClient().readLine());
//        }
//
//        for(int i=0;i<10000;i++){
//            Client.getClient().write("delete from one where id="+i+"\n");
//            System.out.println(Client.getClient().readLine());
//        }
    }
}

