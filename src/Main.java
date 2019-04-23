import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.Client;
import comm.Server;
import comm.Worker;
import queries.misc.selectresultprotocol.Header;
import queries.misc.selectresultprotocol.Page;
import queries.misc.selectresultprotocol.Row;

import java.io.*;
import java.util.ArrayList;


public class Main {

    public static void main(String[] args) throws IOException {
            Worker worker = new Worker();
            Server server = new Server(1695, worker);
            server.start();

            Client.getClient().write("Use Emese" + "\n");
            System.out.println(Client.getClient().readLine());

            Client.getClient().write("select * FROM Indextest2 where  id <500000 AND index1 > 5 AND index2 <5 AND regular > 5 "+"\n");
            System.out.println(Client.getClient().readLine());//read READY

            XmlMapper xmlMapper = new XmlMapper();
            Header readHeader = xmlMapper.readValue(Client.getClient().readLine(), Header.class);
            for (int i = 0; i < readHeader.getPageNumber(); i++) {
                Page page = xmlMapper.readValue(Client.getClient().readLine(), Page.class);
                for (Row row : page.getRows()) {
                    System.out.println(row.getValues());
                }
            }
            System.out.println(Client.getClient().readLine());//read OK

//          Client.getClient().write("DROP TABLE Indextest2"+"\n");
//        System.out.println(Client.getClient().readLine());

//            Client.getClient().write("CREATE TABLE Indextest2 ( id int PK , index1 int , index2 int , regular string )" + "\n");
//            System.out.println(Client.getClient().readLine());
//            int n = 1000000;
//            for (int i = 1; i <= n; i++) {
//                Client.getClient().write("insert into Indextest2 values ( " + i + " , " + i % 10 + " , " + i / 10 % 10 + " , " + i / 100 % 10 + "  )" + "\n");
//            }
//            for (int i = 1; i <= n; i++) {
//                System.out.println(Client.getClient().readLine());
//            }
//
//            Client.getClient().write("CREATE INDEX Indextest2.index1" + "\n");
//            Client.getClient().write("CREATE INDEX Indextest2.index2" + "\n");
//            System.out.println(Client.getClient().readLine());
//            System.out.println(Client.getClient().readLine());

//        for(int i=0;i<1000;i++){
//            Client.getClient().write("delete from Indextest2 where id="+i+"\n");
//            System.out.println(Client.getClient().readLine());
//        }
    }
}

