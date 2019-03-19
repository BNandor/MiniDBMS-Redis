package queries.misc;

import comm.ServerException;
import comm.Worker;
import persistence.XML;
import struct.ForeignKey;
import struct.IndexFile;
import struct.Table;

import java.util.Set;

public class ConstraintChecker {

    public static String anyRowIsBeingReferenced(Table table) throws ServerException {
            Worker.RDB.select(table.getSlotNumber());
            for(String key:Worker.RDB.getAllKeys()){

                if(Integer.parseInt(Worker.RDB.getColumn(key,Worker.referenceCountName))>0){
                    return key;
                }
            }
            return null;
    }

    public static void decrementReferencedColumns(Table table) throws ServerException {
        if (table.getForeignKeys()!=null) {
            for (ForeignKey fk : table.getForeignKeys().getForeignKeyList()) {//for every foreign key
                for (IndexFile index : table.getIndexFiles().getIndexFiles()) {
                    if (index.getName().equals(fk.getName())) {
                        int slot = index.getIndexFileName();

                        Worker.RDB.select(slot);
                        for (String key : Worker.RDB.getAllKeys()) {
                            long number_of_references = Worker.RDB.getSizeOfSet(key);
                            int referenceTableSlot = XML.getTable(fk.getRefTableName(), Worker.currentlyWorking).getSlotNumber();
                            Worker.RDB.select(referenceTableSlot);
                            Worker.RDB.increaseReferenceCount(key, -number_of_references);
                            Worker.RDB.select(slot);
                        }
                        break;
                    }
                }
            }
        }
    }
}
