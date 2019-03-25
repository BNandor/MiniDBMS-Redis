package queries.misc;

import comm.ServerException;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import struct.IndexFile;
import struct.Table;

public class CreateIndex {

    private static CreateIndex instance;

    private CreateIndex(){

    }

    public void createIndex(Table t, String attribute) throws comm.ServerException {

        Worker.RDB.select(0);

        if(XML.hasIndex(t.getTableName(),attribute,Worker.currentlyWorking)){
            throw new comm.ServerException("Error creating index, it is already present");
        }

        boolean set=false;

        for (int i = 1; i < RedisConnector.num_of_tables; i++) {
            if (Worker.RDB.get(i + "") == null) {
                Worker.RDB.set(i + "", "taken");
                set = true;
                t.getIndexFiles().getIndexFiles().add(new IndexFile(i, attribute));
                break;
            }
        }

        if (!set) {
            throw new comm.ServerException("Error creating index file, no free slot left in database");
        }
        Worker.RDB.save();
    }

    public static CreateIndex getInstance(){
        if(instance == null){
            synchronized (CreateIndex.class){
                if(instance == null){
                    instance = new CreateIndex();
                }
            }
        }
        return instance;
    }
}
