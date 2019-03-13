package structbuilder;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import comm.Server;
import comm.Worker;
import persistence.RedisConnector;
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
                    ForeignKey fk = new ForeignKey(attr.getName(),dot.nextToken(),dot.nextToken());
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

        Worker.RDB.save();
        //Loop over unique attributes

        IndexFiles indexFiles = new IndexFiles();

        for(Unique u:uniqueAttributes.getUniqueList()){
            for(int i=1;i<RedisConnector.num_of_tables;i++){
                if(Worker.RDB.get(i+"")==null){
                    Worker.RDB.set(i+"","taken");
                    indexFiles.getIndexFiles().add(new IndexFile(i,u.getName()));
                    break;
                }
            }
        }
        Worker.RDB.save();
        return new Table(name,table_num,0,structure,pk,fks,uniqueAttributes,indexFiles);
    }
}
