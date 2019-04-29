package queries.misc;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import comm.ServerException;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import struct.IndexFile;
import struct.Table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CreateIndex {

    private static CreateIndex instance;
    private Integer resultsPerQuery = 1000;

    private CreateIndex() {

    }

    public void createIndex(Table t, String attribute) throws comm.ServerException {

        Worker.RDB.select(0);

        if (XML.hasIndex(t.getTableName(), attribute, Worker.currentlyWorking)) {
            throw new comm.ServerException("Error creating index, it is already present");
        }

        boolean set = false;
        int i;
        for ( i = 1; i < RedisConnector.num_of_tables; i++) {
            if (Worker.RDB.get(i + "") == null) {
                Worker.RDB.set(i + "", "index");
                set = true;
                t.getIndexFiles().getIndexFiles().add(new IndexFile(i, attribute));
                break;
            }
        }

        if (!set) {
            throw new comm.ServerException("Error creating index file, no free slot left in database");
        }
        fillCreatedIndex(t, attribute, i);
    }

    private void fillCreatedIndex(Table t, String attribute, int indexSlot) {
        Worker.RDB.select(t.getSlotNumber());
        String cursor = "0";
        ScanParams params = new ScanParams();
        params.count(resultsPerQuery);
        ScanResult<String> result= Worker.RDB.scan(cursor,params);// scan can return an element multiple times,
                            // but since we are saving to a set, duplicates don't matter
        do{
            //result has a set of keys on which we can iterate
            List<String> attributeValues=new ArrayList<>();//get attribute values at current keys
            for(String key:result.getResult()){
                attributeValues.add(Worker.RDB.getColumn(key,attribute));
            }

            Worker.RDB.select(indexSlot);
            int i=0;
            for(String key:result.getResult()){
                Worker.RDB.addToSet(attributeValues.get(i),key);
                i++;
            }
            Worker.RDB.select(t.getSlotNumber());
            cursor = result.getCursor();
            result = Worker.RDB.scan(cursor,params);
        }while(!result.getCursor().equals("0"));
        List<String> attributeValues=new ArrayList<>();//get attribute values at current keys
        for(String key:result.getResult()){
            attributeValues.add(Worker.RDB.getColumn(key,attribute));
        }

        Worker.RDB.select(indexSlot);
        int i=0;
        for(String key:result.getResult()){
            Worker.RDB.addToSet(attributeValues.get(i),key);
            i++;
        }
    }

    public static CreateIndex getInstance() {
        if (instance == null) {
            synchronized (CreateIndex.class) {
                if (instance == null) {
                    instance = new CreateIndex();
                }
            }
        }
        return instance;
    }
}
