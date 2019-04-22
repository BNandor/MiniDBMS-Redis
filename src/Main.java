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

        Client.getClient().write("Use Emese"+"\n");
        Client.getClient().write("select * FROM Indextest2 where index2 = 0 "+"\n");
//        Client.getClient().write("DROP TABLE Indextest2"+"\n");
//        Client.getClient().write("CREATE TABLE Indextest2 ( id int PK , index1 int , index2 int , regular string )"+"\n");
//
//        for(int i=1;i<=10000;i++){
//            Client.getClient().write("insert into Indextest2 values ( "+i+" , "+i%10+" , "+i/10%10+" , "+i/100%10+"  )"+"\n");
//            System.out.println(Client.getClient().readLine());
//        }
//
//        Client.getClient().write("CREATE INDEX Indextest2.index1"+"\n");
//        Client.getClient().write("CREATE INDEX Indextest2.index2"+"\n");
//
        System.out.println(Client.getClient().readLine());
        System.out.println(Client.getClient().readLine());
//        System.out.println(Client.getClient().readLine());
//        System.out.println(Client.getClient().readLine());
//        for(int i=0;i<1000;i++){
//            Client.getClient().write("delete from Indextest2 where id="+i+"\n");
//            System.out.println(Client.getClient().readLine());
//        }
    }
}

