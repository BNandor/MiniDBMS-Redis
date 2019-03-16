package queries;

import comm.Worker;
import persistence.XML;
import struct.*;

import java.util.ArrayList;
import java.util.StringTokenizer;

import static java.util.stream.Collectors.toList;



public class InsertQuery {

    public static void insert(Table table,StringTokenizer tokenizer) throws comm.ServerException {//TODO optimize column checking, maybe memoize constraints within attributes
        tokenizer.nextToken();//read (
        ArrayList<InputEntry> input= new ArrayList<>();
        ArrayList<UniqueEntry> uniqueEntries = new ArrayList<>();
        ArrayList<ForeignKeyEntry> foreignKeyEntries = new ArrayList<>();
        String element,primaryKey=null;
        Structure tableStructure = table.getTableStructure();
        Worker.RDB.select(table.getSlotNumber());

        for (Attribute attr : tableStructure.getAttributeList()){
            element = tokenizer.nextToken();//next input
            if(attr.getName().equals(table.getKey().getName())){//if this is the primary key
                if(Worker.RDB.keyExists(element)){
                    throw new comm.ServerException("Error on inserting into "+table.getTableName()+" primary key "+element+" , value already present in database");
                }
                primaryKey=element;
            }else{

                boolean set=false;
                if(table.getForeignKeys()!=null) {
                    for (ForeignKey fk : table.getForeignKeys().getForeignKeyList()) {
                        if (fk.getName().equals(attr.getName())) {
                            set = true;
                            int referenceTableSlot = XML.getTable(fk.getRefTableName(), Worker.currentlyWorking).getSlotNumber();
                            Worker.RDB.select(referenceTableSlot);//TODO memoize table slot of referenced foreign table
                            if (!Worker.RDB.keyExists(element)) {//TODO if referenced key does not exist, extend here if foreign key can point to unique
                                Worker.RDB.select(table.getSlotNumber());//TODO maybe optional
                                throw new comm.ServerException("Error inserting into " + table.getTableName() + " foreign key " + fk.getName() + element + " does not exist in referenced table"+fk.getRefTableName());
                            }

                            int indexSlot = ((ArrayList<IndexFile>) (table.getIndexFiles().getIndexFiles().stream().filter(t -> t.getName().equals(attr.getName())).collect(toList()))).get(0).getIndexFileName();
                            foreignKeyEntries.add(new ForeignKeyEntry(indexSlot, element, referenceTableSlot));
                            Worker.RDB.select(table.getSlotNumber());//TODO maybe optional
                            break;
                        }
                    }
                }

                if(!set){
                    if(table.getUniqeAttributes()!=null){
                    for (Unique unique:table.getUniqeAttributes().getUniqueList()){
                        if(unique.getName().equals(attr.getName())){//if current attribute is of type unique, check whether that value already exists

                            int indexSlot=((ArrayList<IndexFile>)(table.getIndexFiles().getIndexFiles().stream().filter(t->t.getName().equals(attr.getName())).collect(toList()))).get(0).getIndexFileName();
                            Worker.RDB.select(indexSlot);
                                if(Worker.RDB.keyExists(element)){
                                    Worker.RDB.select(table.getSlotNumber());//TODO maybe optional
                                    throw new comm.ServerException("Error inserting into "+table.getTableName()+" unique attribute "+attr.getName()+" already has a value of "+element);
                                }
                            uniqueEntries.add(new UniqueEntry(indexSlot,element));
                            Worker.RDB.select(table.getSlotNumber());//TODO maybe optional
                            break;
                        }
                    }
                    }
                }

                input.add(new InputEntry(attr.getName(),element));
            }


            tokenizer.nextToken();//read next , (comma) or )
        }

        // at this point, everything should be fine
        //TODO update index files
            //Insert unique values into index files
            for(UniqueEntry entry:uniqueEntries){
                Worker.RDB.select(entry.slot);
                Worker.RDB.set(entry.key,primaryKey);
            }

            //Insert primary key value into foreign key index
            for(ForeignKeyEntry entry:foreignKeyEntries){
                Worker.RDB.select(entry.slot);
                Worker.RDB.addToSet(entry.key,primaryKey);
                Worker.RDB.select(entry.referencedTableSlot);//TODO update referenced column in referenced table
                Worker.RDB.increaseReferenceCount(entry.key);
            }
        //TODO insert values into table
        Worker.RDB.select(table.getSlotNumber());
        for(InputEntry entry:input){
            Worker.RDB.setColumn(primaryKey,entry.attributeName,entry.value);
        }
        //Insert referenced column
        Worker.RDB.setColumn(primaryKey,Worker.referenceCountName,0+"");
        //save all of the changes
        Worker.RDB.save();
    }

    private static class UniqueEntry{
            public int slot;
            public String key;
        public UniqueEntry(int slot, String key) {
            this.slot = slot;
            this.key = key;
        }
    }
    private static class ForeignKeyEntry{
        public int slot;
        public String key;
        public int referencedTableSlot;

        public ForeignKeyEntry(int slot, String key,int referencedTableSlot) {
            this.slot = slot;
            this.key = key;
            this.referencedTableSlot=referencedTableSlot;
        }
    }
    private static class InputEntry{
        public InputEntry(String attributeName, String value) {
            this.attributeName = attributeName;
            this.value = value;
        }

        public String attributeName;
        public String value;
    }
}
