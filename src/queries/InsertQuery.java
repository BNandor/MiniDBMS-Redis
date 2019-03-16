package queries;

import comm.Server;
import comm.ServerException;
import comm.Worker;
import persistence.XML;
import struct.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static java.util.stream.Collectors.toList;

public class InsertQuery {

    public static void insert(Table table,StringTokenizer tokenizer) throws comm.ServerException {//TODO optimize column checking, maybe memoize constraints within attributes
        tokenizer.nextToken();//read (
        ArrayList<String> input= new ArrayList<>();
        String element;
        Structure tableStructure = table.getTableStructure();
        Worker.RDB.select(table.getSlotNumber());

        for (Attribute attr : tableStructure.getAttributeList()){
            element = tokenizer.nextToken();//next input
            if(attr.getName().equals(table.getKey().getName())){//if this is the primary key
                if(Worker.RDB.keyExists(element)){
                    throw new comm.ServerException("Error on inserting into "+table.getTableName()+" primary key "+input+" , value already present in database");
                }
            }else{
                boolean set=false;
                for (ForeignKey fk:table.getForeignKeys().getForeignKeyList()){
                    if(fk.getName().equals(attr.getName())){
                        set=true;
                            Worker.RDB.select(XML.getTable(fk.getRefTableName(),Worker.currentlyWorking).getSlotNumber());//TODO memoize table slot of referenced foreign table
                                if(!Worker.RDB.keyExists(element)){//TODO if referenced key does not exist, extend here if foreign key can point to unique
                                    Worker.RDB.select(table.getSlotNumber());//TODO maybe optional
                                    throw new comm.ServerException("Error inserting into "+table.getTableName()+" foreign key "+fk.getName()+" does not exist in referenced table");
                                }
                        Worker.RDB.select(table.getSlotNumber());
                        break;
                    }
                }

                if(!set){
                    for (Unique unique:table.getUniqeAttributes().getUniqueList()){
                        if(unique.getName().equals(attr.getName())){//if current attribute is of type unique, check whether that value already exists

                            int indexSlot=((ArrayList<IndexFile>)(table.getIndexFiles().getIndexFiles().stream().filter(t->t.getName().equals(attr.getName())).collect(toList()))).get(0).getIndexFileName();
                            Worker.RDB.select(indexSlot);
                                if(Worker.RDB.keyExists(element)){
                                    Worker.RDB.select(table.getSlotNumber());
                                    throw new comm.ServerException("Error inserting into "+table.getTableName()+" unique attribute "+attr.getName()+" already has a value of "+element);
                                }
                            Worker.RDB.select(table.getSlotNumber());
                            break;
                        }
                    }
                }

            }

            input.add(element);
            tokenizer.nextToken();//read next , (comma) or )
        }

        // at this point, everything should be fine
        //TODO update index files
        
        //TODO update referenced column in referenced table
        //TODO insert values in table

    }
}
