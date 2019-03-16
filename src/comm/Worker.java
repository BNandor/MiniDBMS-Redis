package comm;

import persistence.RedisConnector;
import persistence.XML;
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
import java.util.Queue;
import java.util.StringTokenizer;

public class Worker extends Thread {
    public static final String killSwitch="exit";
    public static final String path_to_work = "DBMS";
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
    private boolean usingDatabase(){
        try {
            return currentlyWorking!=-1 && currentlyWorking< XML.getDatabasesInstance().getDatabaseList().size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            while (jobs.size() == 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String query = jobs.poll();

            try {


                switch (query) {


                    case killSwitch:{
                        if(RDB.running()){
                            RDB.killServer();
                        }
                        System.exit(0);
                    }
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
                            case "create": {
                                switch (object.toLowerCase()) {
                                    case "database": {
                                        String name = tokenizer.nextToken();

                                        try {

                                            for (Database d : XML.getDatabasesInstance().getDatabaseList()) {
                                                if(d.getDatabaseName().equals(name)){
                                                    throw new ServerException("Cannot create database "+ name+" it is already present");
                                                }
                                            }
                                            Database d = new Database();
                                            d.setDatabaseName(name);
                                            XML.getDatabasesInstance().getDatabaseList().add(d);
                                            XML.flush();
                                            DatabaseBuilder.createDatabase(name);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            throw new ServerException("Cannot find xml file:" + e.getMessage());
                                        }
                                    }
                                    break;

                                    case "table": {//create table 'name' (id int, )

                                        try {

                                            if (!usingDatabase()) {
                                                throw new ServerException("Please USE a database");
                                            }

                                            String name = tokenizer.nextToken();//Check if table is already present in database
                                            for (Table t : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList()) {
                                                if (t.getTableName() != null && t.getTableName().equals(name)) {
                                                    throw new ServerException("Cannot create table "+name+" it is already present");
                                                }
                                            }

                                            XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().add(TableBuilder.getTable(name,tokenizer));
                                            XML.flush();

                                        } catch (FileNotFoundException | ServerException e) {
                                            e.printStackTrace();
                                            throw new ServerException("Error creating table:" + e.getMessage());
                                        }

                                    }
                                }
                            }
                            break;
                            case "insert": { //insert into table values ( val1 , val2 , val3 )
                                //TODO get insert
                                //parse values
                                String tableName = tokenizer.nextToken();//
                                tokenizer.nextToken();


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
                                                throw new ServerException("Error dropping database " + name + ": Does not exist");
                                            }

                                            if (currentlyWorking == i) {
                                                System.out.println("Worker:trying to delete database in use");
                                                //TODO solve this issue
                                                currentlyWorking = -1;
                                            }

                                            if (currentlyWorking > i) {
                                                --currentlyWorking;
                                            }

                                            XML.getDatabasesInstance().getDatabaseList().remove(i);
                                            XML.flush();
                                            DatabaseBuilder.deleteDatabase(name);
                                            //TODO maybe delete everything from REDIS as well
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();

                                        }
                                    }

                                    break;
                                    case "table": {
                                        String name = tokenizer.nextToken();
                                        int i = 0;
                                        try {
                                            for (Table t : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList()) {
                                                if (t.getTableName() != null && t.getTableName().equals(name)) {
                                                    break;
                                                }
                                                ++i;
                                            }

                                            if (i == XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().size()) {
                                                throw new ServerException("Cannot delete table " + name + " it does not exist");
                                            }

                                            if (!RDB.running()) {
                                                System.out.println("Worker:Error: drop table redis not running");
                                                throw new ServerException("Cannot drop table " + name + " Redis not running");
                                            }

                                            RDB.select(XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getSlotNumber());
                                            RDB.dropselected();
                                            if (XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getIndexFiles() != null) {
                                                for (IndexFile indexFile : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getIndexFiles().getIndexFiles()) {
                                                    RDB.select(indexFile.getIndexFileName());
                                                    RDB.dropselected();
                                                }
                                            }

                                            RDB.select(0);
                                            if (XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getIndexFiles() != null) {
                                                for (IndexFile indexFile : XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getIndexFiles().getIndexFiles()) {
                                                    RDB.delkey(indexFile.getIndexFileName() + "");
                                                }
                                            }
                                            RDB.delkey(XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().get(i).getSlotNumber() + "");
                                            RDB.save();

                                            XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().remove(i);
                                            XML.flush();

                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                            throw new ServerException(e.getMessage());
                                        }

                                    }
                                    break;
                                }
                            }
                            break;

                            case "use": {

                                try {

                                    //If we are already using desired database, do nothing

                                    if(usingDatabase() && XML.getDatabasesInstance().getDatabaseList().get(currentlyWorking).getDatabaseName().equals(object)){
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
                                        throw new ServerException("Cannot use database " + object + " as it does not exist");
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
            } catch (ServerException ex) {
                //If anything goes wrong, send the message
                messageSender.write(ex.getMessage() + "\n");
                messageSender.flush();
            }
        }
    }
}
