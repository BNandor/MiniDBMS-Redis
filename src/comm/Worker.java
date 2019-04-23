package comm;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import persistence.RedisConnector;
import persistence.XML;
import queries.DeleteQuery;
import queries.InsertQuery;
import queries.SelectQuery;
import queries.misc.ConstraintChecker;
import queries.misc.CreateIndex;
import struct.Database;
import struct.IndexFile;
import struct.Table;
import structbuilder.DatabaseBuilder;
import structbuilder.TableBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.ServerException;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.StringTokenizer;

public class Worker extends Thread {
    public static final String killSwitch = "exit";
    public static final String path_to_work = "DBMS";
    public static final String referenceCountName = "__referenced__";
    private Queue<String> jobs;
    private Socket clientSocket;
    private PrintWriter messageSender;
    public static int currentlyWorking;
    public static RedisConnector RDB;


    public Worker() {
        currentlyWorking = -1;
        jobs = new ArrayDeque<>();
        RDB = new RedisConnector();

    }

    public void addJob(String job) {
        jobs.add(job);
    }

    public void setSocket(Socket sock) {
        clientSocket = sock;
        try {
            messageSender = new PrintWriter(sock.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean usingDatabase() {
        try {
            return currentlyWorking != -1 && currentlyWorking < XML.getDatabasesInstance().getDatabaseList().size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void run() {
        super.run();
        boolean dbchanged=false;
        while (true) {
            while (jobs.size() == 0) {
                try {
                    if(usingDatabase() && dbchanged) {
                        RDB.save();
                        dbchanged=false;
                        System.out.println("REDIS SAVED");
                    }
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String query = jobs.poll();

            try {


                switch (query) {


                    case killSwitch: {
                        if (RDB.running()) {
                            RDB.killServer();
                        }
                        System.exit(0);
                    }break;
                    case Client.getDatabasesQuery: {
                        XML.flush();
                        messageSender.write(XML.readXML() + "\n");
                        messageSender.flush();
                    }
                    break;
                    default: {
                        StringTokenizer tokenizer = new StringTokenizer(query);

                        String method = tokenizer.nextToken(), object = tokenizer.nextToken();
                        switch (method.toLowerCase()) {
                            case "select":{
                                if (!usingDatabase()) {
                                    throw new comm.ServerException("Please USE a database");
                                }
                                SelectQuery selectQuery =  new SelectQuery(query,messageSender);
                                selectQuery.writeResult(selectQuery.select(selectQuery.buildQuery()));
                            }break;
                            case "delete":{
                                try {
                                    if (!usingDatabase()) {
                                        throw new comm.ServerException("Please USE a database");
                                    }
                                    String tableName = tokenizer.nextToken();//Table

                                    if(!XML.tableExists(tableName,currentlyWorking)){
                                        throw new comm.ServerException("Error: table "+tableName+" does not exist int database");
                                    }

                                    //TODO implement delete from table //this means everything
                                    String primaryKeyValue = query.split("=")[1];
                                    Table t = XML.getTable(tableName,currentlyWorking);
                                    DeleteQuery.getInstance().delete(t,primaryKeyValue);
                                    dbchanged=true;
                                }catch(NoSuchElementException |IndexOutOfBoundsException ex){
                                    throw new comm.ServerException("Error deleting row(s), syntax error");
                                }
                            }break;
                            case "create": {
                                switch (object.toLowerCase()) {
                                    case "database": {
                                        String name = tokenizer.nextToken();

                                        try {

                                            for (Database d : XML.getDatabasesInstance().getDatabaseList()) {
                                                if (d.getDatabaseName().equals(name)) {
                                                    throw new comm.ServerException("Cannot create database " + name + " it is already present");
                                                }
                                            }
                                            Database d = new Database();
                                            d.setDatabaseName(name);
                                            XML.getDatabasesInstance().getDatabaseList().add(d);
                                            XML.flush();
                                            DatabaseBuilder.createDatabase(name);
                                            dbchanged=true;
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            throw new comm.ServerException("Cannot find xml file:" + e.getMessage());
                                        }
                                    }
                                    break;

                                    case "table": {//create table 'name' (id int, )

                                        try {

                                            if (!usingDatabase()) {
                                                throw new comm.ServerException("Please USE a database");
                                            }

                                            String name = tokenizer.nextToken();//Check if table is already present in database
                                            for (Table t : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList()) {
                                                if (t.getTableName() != null && t.getTableName().equals(name)) {
                                                    throw new comm.ServerException("Cannot create table " + name + " it is already present");
                                                }
                                            }

                                            XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().add(TableBuilder.getTable(name, tokenizer));
                                            XML.flush();
                                            dbchanged=true;
                                        } catch (FileNotFoundException | comm.ServerException e) {
                                            e.printStackTrace();
                                            throw new comm.ServerException("Error creating table:" + e.getMessage());
                                        }
                                    }
                                    break;
                                    case "index": {//CREATE  INDEX Table.Column
                                        StringTokenizer dotTokenizer = new StringTokenizer(tokenizer.nextToken(), ".");
                                        try {
                                            String tableName = dotTokenizer.nextToken();
                                            String columnName = dotTokenizer.nextToken();
                                            //check whether we are using a database
                                            if (!usingDatabase()) {
                                                throw new comm.ServerException("Error creating index file, not using any database");
                                            }
                                            if (!XML.tableExists(tableName, currentlyWorking)) {
                                                throw new comm.ServerException("Error creating index file, table " + tableName + " does not exist");
                                            }
                                            if (!XML.attributeExists(tableName, columnName, currentlyWorking)) {
                                                throw new comm.ServerException("Error creating index file, column " + columnName + " does not existr");
                                            }
                                            //check whether column is unique, or foreign key, because for these types there already is an index file created automagically
                                            if (XML.attributeIsUnique(tableName, columnName, currentlyWorking)) {
                                                throw new comm.ServerException("Error creating index file, column " + columnName + " is unique, there already exists an index file for it");
                                            }
                                            if (XML.attributeIsForeignKey(tableName, columnName, currentlyWorking)) {
                                                throw new comm.ServerException("Error creating index file, column " + columnName + " is foreign key, there already exists an index file for it");
                                            }
                                            if (XML.attributeIsPrimaryKey(tableName, columnName, currentlyWorking)) {
                                                throw new comm.ServerException("Error creating index file, column " + columnName + " is primary key, it cannot be indexed");
                                            }

                                            Table t = XML.getTable(tableName,currentlyWorking);
                                            CreateIndex.getInstance().createIndex(t,columnName);
                                            XML.flush();
                                            dbchanged=true;
                                        } catch (NoSuchElementException ex) {
                                            throw new comm.ServerException("Syntax error in create index");
                                        }

                                    }
                                }
                            }

                            break;
                            case "insert": { //insert into table values ( val1 , val2 , val3 )
                                String tableName = null;

                                try {
                                    tableName = tokenizer.nextToken();//table
                                    tokenizer.nextToken();//values
                                } catch (Exception e) {
                                    throw new comm.ServerException("Error inserting into table, syntax error");
                                }
                                Table insertTable = XML.getTable(tableName, currentlyWorking);
                                if (insertTable == null) {
                                    throw new comm.ServerException("Error inserting into table, table " + tableName + " does not exist");
                                }
                                try {
                                    InsertQuery.insert(insertTable, tokenizer);
                                    dbchanged=true;
                                } catch (NoSuchElementException ex) {
                                    throw new comm.ServerException("You have a syntax error in your insert somewhere");
                                }
                            }
                            break;
                            case "drop": {
                                switch (object.toLowerCase()) {
                                    case "database": {
                                        String name = tokenizer.nextToken();
                                        int i = 0;
                                        try {

                                            for (Database d : XML.getDatabasesInstance().getDatabaseList()) {
                                                if (d.getDatabaseName() != null && d.getDatabaseName().equals(name)) {
                                                    break;
                                                }
                                                ++i;
                                            }

                                            if (i == XML.getDatabasesInstance().getDatabaseList().size()) {
                                                throw new comm.ServerException("Error dropping database " + name + ": Does not exist");
                                            }

                                            if (currentlyWorking == i) {
                                                System.out.println("Worker:trying to delete database in use");
                                                RDB.killServer();
                                                currentlyWorking = -1;
                                            }

                                            if (currentlyWorking > i) {
                                                --currentlyWorking;
                                            }

                                            XML.getDatabasesInstance().getDatabaseList().remove(i);
                                            XML.flush();
                                            DatabaseBuilder.deleteDatabase(name);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                    case "table": {
                                        String name = tokenizer.nextToken();
                                        int i = 0;
                                        try {

                                            if (!usingDatabase()) {
                                                throw new comm.ServerException("Error dropping table" + name + ", not using any database");
                                            }

                                            for (Table t : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList()) {
                                                if (t.getTableName() != null && t.getTableName().equals(name)) {
                                                    break;
                                                }
                                                ++i;
                                            }

                                            if (i == XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().size()) {
                                                throw new comm.ServerException("Cannot delete table " + name + " it does not exist");
                                            }

                                            if (!RDB.running()) {
                                                System.out.println("Worker:Error: drop table redis not running");
                                                throw new comm.ServerException("Cannot drop table " + name + " Redis not running");
                                            }

                                            Table currentTable = XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i);
                                            String referencedKey = ConstraintChecker.anyRowIsBeingReferenced(currentTable);

                                            if (referencedKey != null) {
                                                throw new comm.ServerException("Cannot drop table , key  " + referencedKey + "  is being referenced  from another table");
                                            }

                                            //at this point nothing is being referenced, we're good to go
                                            ConstraintChecker.decrementReferencedColumns(currentTable);

                                            RDB.select(currentTable.getSlotNumber());
                                            RDB.dropselected();
                                            if (currentTable.getIndexFiles() != null) {
                                                for (IndexFile indexFile : currentTable.getIndexFiles().getIndexFiles()) {
                                                    RDB.select(indexFile.getIndexFileName());
                                                    RDB.dropselected();
                                                }
                                            }

                                            RDB.select(0);
                                            if (currentTable.getIndexFiles() != null) {
                                                for (IndexFile indexFile : currentTable.getIndexFiles().getIndexFiles()) {
                                                    RDB.delkey(indexFile.getIndexFileName() + "");
                                                }
                                            }
                                            RDB.delkey(currentTable.getSlotNumber() + "");
                                            RDB.save();

                                            XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().remove(i);
                                            XML.flush();
                                            dbchanged=true;
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            throw new comm.ServerException(e.getMessage());
                                        }

                                    }
                                    break;
                                }
                            }
                            break;

                            case "use": {

                                try {

                                    //If we are already using desired database, do nothing

                                    if (usingDatabase() && XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getDatabaseName().equals(object)) {
                                        break;
                                    }

                                    int i = 0;

                                    for (Database d : XML.getDatabasesInstance().getDatabaseList()) {

                                        if (d.getDatabaseName() != null && d.getDatabaseName().equals(object)) {
                                            currentlyWorking = i;
                                            break;
                                        }
                                        ++i;
                                    }

                                    if (i == XML.getDatabasesInstance().getDatabaseList().size()) {
                                        throw new comm.ServerException("Cannot use database " + object + " as it does not exist");
                                    }

                                    if (RDB.running()) {
                                        RDB.killServer();
                                        RDB = new RedisConnector();
                                    }

                                    RDB.start();
                                    RDB.connect();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        }
                    }
                }

                //ON ok, send OK
                messageSender.write("OK" + "\n");
                messageSender.flush();
            } catch (comm.ServerException ex) {
                //If anything goes wrong, send the message
                messageSender.write(ex.getMessage() + "\n");
                messageSender.flush();
            }
        }
    }
}
