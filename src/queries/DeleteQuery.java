package queries;

import com.ctc.wstx.util.WordResolver;
import comm.ServerException;
import comm.Worker;
import persistence.XML;
import struct.ForeignKey;
import struct.IndexFile;
import struct.Table;
import struct.Unique;

public class DeleteQuery {
    private static DeleteQuery instance;
    private DeleteQuery() {}
    public static DeleteQuery getInstance(){
        if(instance==null){
            synchronized (DeleteQuery.class){
                if(instance==null){
                    instance = new DeleteQuery();
                }
            }
        }
        return instance;
    }

    public void delete(Table table, String primaryKeyValue) throws ServerException {

        Worker.RDB.select(table.getSlotNumber());
        if(!Worker.RDB.keyExists(primaryKeyValue)){//if primary key does not exist
            return;
        }
        //check if row is being referenced from another table
        if(Integer.parseInt(Worker.RDB.getColumn(primaryKeyValue,Worker.referenceCountName))>0){
            throw new comm.ServerException("Error deleting row "+primaryKeyValue+" ,it is being referenced from another table");
        }

        if (table.getForeignKeys()!=null) {
            for (ForeignKey fk : table.getForeignKeys().getForeignKeyList()) {//for every foreign key
                Worker.RDB.select(table.getSlotNumber());
                String fkval = Worker.RDB.getColumn(primaryKeyValue,fk.getName());
                for (IndexFile index : table.getIndexFiles().getIndexFiles()) {
                    if (index.getName().equals(fk.getName())) {
                        Worker.RDB.select(index.getIndexFileName());
                        Worker.RDB.removeFromSet(fkval,primaryKeyValue);
                        break;
                    }
                }//updated index file
                //decrease reference count in other table
                Worker.RDB.select(XML.getTable(fk.getRefTableName(),Worker.currentlyWorking).getSlotNumber());
                //TODO if key that is referenced is unique, implement the search for that
                Worker.RDB.increaseReferenceCount(fkval,-1);
            }
        }

        if(table.getUniqeAttributes()!=null){
            for(Unique unique:table.getUniqeAttributes().getUniqueList()){
                Worker.RDB.select(table.getSlotNumber());
                String uqval = Worker.RDB.getColumn(primaryKeyValue,unique.getName());
                for (IndexFile index : table.getIndexFiles().getIndexFiles()) {
                    if (index.getName().equals(unique.getName())) {
                        Worker.RDB.select(index.getIndexFileName());
                        Worker.RDB.delkey(uqval);
                        break;
                    }
                }
            }
        }
        if (table.getIndexFiles() !=null) {
            for (IndexFile index : table.getIndexFiles().getIndexFiles()) {
                if (!XML.attributeIsForeignKey(table.getTableName(), index.getName(), Worker.currentlyWorking) && !XML.attributeIsUnique(table.getTableName(), index.getName(), Worker.currentlyWorking)) {
                    //ordinary index file
                    Worker.RDB.select(table.getSlotNumber());
                    String val = Worker.RDB.getColumn(primaryKeyValue, index.getName());

                    Worker.RDB.select(index.getIndexFileName());
                    Worker.RDB.removeFromSet(val, primaryKeyValue);
                }
            }
        }
        Worker.RDB.select(table.getSlotNumber());
        Worker.RDB.delkey(primaryKeyValue);
    }
}
