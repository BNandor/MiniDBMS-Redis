package structbuilder;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import comm.Server;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import struct.*;

import java.rmi.ServerException;
import java.util.StringTokenizer;

public class TableBuilder {
    public static Table getTable(String name,StringTokenizer tokenizer) throws ServerException{

        Structure structure = new Structure();
        String element;

        ForeignKeys fks = new ForeignKeys();
        UniqueAttributes uniqueAttributes = new UniqueAttributes();
        String primaryKey=null;

        tokenizer.nextToken();//get initial (

        while(tokenizer.hasMoreTokens()){
            Attribute attr = new Attribute();
            attr.setIsnull(0);
            element=tokenizer.nextToken();//get name of field


            attr.setName(element);
            element=tokenizer.nextToken();//get type of field
            attr.setType(element);
            element=tokenizer.nextToken();//get comma, check if pk, or foreign key
            if(element.equals("PK")){
                primaryKey=attr.getName();
                tokenizer.nextToken();
            }else{
                if(element.equals("FK")){

                    element=tokenizer.nextToken();//foreign table.column
                    StringTokenizer dot = new StringTokenizer(element,".");
                    String refTable,refAttr;
                    refTable=dot.nextToken();
                    refAttr=dot.nextToken();

                    if(!XML.tableExists(refTable,Worker.currentlyWorking)){
                        throw new ServerException("Referenced table does not exist "+refTable);
                    }
                    //TODO check if referenced attribute is of type id, or at least unique
                    if(!XML.attributeExists(refTable,refAttr,Worker.currentlyWorking)){
                        throw new ServerException("Referenced attribute does not exist "+refAttr);
                    }

                    ForeignKey fk = new ForeignKey(attr.getName(),refTable,refAttr);

                    fks.getForeignKeyList().add(fk);
                    tokenizer.nextToken();
                }else{
                    if(element.equals("UNIQUE")){
                        uniqueAttributes.getUniqueList().add(new Unique(attr.getName()));
                        tokenizer.nextToken();
                    }else{
                        if(element.equals("ISNULL")){
                            attr.setIsnull(1);
                            tokenizer.nextToken();
                        }
                    }
                }
            }

            structure.getAttributeList().add(attr);
        }

        if(primaryKey==null){
            System.out.println("TableBuilder: Error, primary key was not given");
            throw new ServerException("TableBuilder: Error, primary key was not given");
        }


        PrimaryKey pk = new PrimaryKey(primaryKey);
        if(Worker.RDB.running()==false){
            System.out.println("TableCreator: Error: trying to create table in db that is not running");
            throw new ServerException("TableCreator: Error: trying to create table in db that is not running");
        }

        Worker.RDB.select(0);
        int table_num=-1;

        for(int i=1;i<RedisConnector.num_of_tables;i++){
            if(Worker.RDB.get(i+"")==null){
                Worker.RDB.set(i+"","taken");
                table_num=i;
                break;
            }
        }

        if(table_num == -1){
            throw new ServerException("Error creating table, no free slot left in database, increase slot size in the conf file please");
        }

        //Worker.RDB.save();
        //Loop over unique attributes and create index tables

        IndexFiles indexFiles = new IndexFiles();
        boolean set;
        for(Unique u:uniqueAttributes.getUniqueList()){
            set=false;
            for(int i=1;i<RedisConnector.num_of_tables;i++){
                if(Worker.RDB.get(i+"")==null){
                    set=true;
                    Worker.RDB.set(i+"","taken");
                    indexFiles.getIndexFiles().add(new IndexFile(i,u.getName()));
                    break;
                }
            }
            if(!set){
                throw new ServerException("Error creating table, no free slot left in database, increase slot size in the conf file please");
            }
        }

        //Worker.RDB.save();
        //Loop over foreign key attributes, and create index files
        for (ForeignKey fk:fks.getForeignKeyList()){
            set=false;
            for(int i=1;i<RedisConnector.num_of_tables;i++){
                if(Worker.RDB.get(i+"")==null){
                    Worker.RDB.set(i+"","taken");
                    set=true;
                    indexFiles.getIndexFiles().add(new IndexFile(i,fk.getName()));
                    break;
                }
            }
            if(!set){
                throw new ServerException("Error creating table, no free slot left in database, increase slot size in the conf file please");
            }
        }

        Worker.RDB.save();
        return new Table(name,table_num,0,structure,pk,fks,uniqueAttributes,indexFiles);
    }
}
