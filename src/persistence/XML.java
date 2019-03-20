package persistence;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.Worker;
import struct.*;

import java.io.*;
import java.rmi.ServerException;
import java.util.ArrayList;

import static java.util.stream.Collectors.toList;

public class XML {

    public static final String databasesName="databases.xml";
    private static XML instance;
    private static Databases databasesInstance;

    public static  String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private XML() {

    }


    public static String readXML(){
        File file = new File(Worker.path_to_work + "/" + databasesName);
        try {
            return inputStreamToString(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    return null;
    }
    public synchronized static Databases getDatabasesInstance() throws FileNotFoundException {
        if (databasesInstance == null) {
            File file = new File(Worker.path_to_work + "/" + databasesName);
            XmlMapper xmlReader = new XmlMapper();
            try {
                databasesInstance = xmlReader.readValue(inputStreamToString(new FileInputStream(file)), Databases.class);
            }catch (IOException ex){
                System.out.println("XML: Error ");
                ex.printStackTrace();
            }
        }
        return databasesInstance;
    }

    public static Table getTable(String tableName,int currentlyWorking) throws comm.ServerException {
        try {
            if(currentlyWorking == -1 || currentlyWorking >= getDatabasesInstance().getDatabaseList().size()){
                throw new comm.ServerException("Server has no selected database");
            }
            ArrayList<Table> list = ((ArrayList<Table>)(getDatabasesInstance().getDatabaseList().get(currentlyWorking).getTables().getTableList().stream().filter(t->t.getTableName().equals(tableName)).collect(toList())));
            if(list.size()==0)return null;
            return list.get(0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean tableExists(String name,int dbindex){
        try {
            if(dbindex == -1 || dbindex >= getDatabasesInstance().getDatabaseList().size()){
                return false;
            }

            for (Table t : getDatabasesInstance().getDatabaseList().get(dbindex).getTables().getTableList()) {
                if (t.getTableName() != null && t.getTableName().equals(name)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean attributeExists(String tableName,String attributeName,int dbindex){

        try {

            if(dbindex == -1 || dbindex >= getDatabasesInstance().getDatabaseList().size()){
                return false;
            }

            for (Table t : getDatabasesInstance().getDatabaseList().get(dbindex).getTables().getTableList()) {
                if (t.getTableName() != null && t.getTableName().equals(tableName)) {
                    for(Attribute attr:t.getTableStructure().getAttributeList()){
                        if(attr.getName()!=null && attr.getName().equals(attributeName)){
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean attributeIsUnique(String tableName,String attributeName, int dbindex){

        try {
            for (Table t : getDatabasesInstance().getDatabaseList().get(dbindex).getTables().getTableList()) {
                if (t.getTableName() != null && t.getTableName().equals(tableName)) {
                    for(Unique unique:t.getUniqeAttributes().getUniqueList()){
                        if(unique.getName()!=null && unique.getName().equals(attributeName)){
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean attributeIsForeignKey(String tableName,String attributeName, int dbindex){
        try {
            for (Table t : getDatabasesInstance().getDatabaseList().get(dbindex).getTables().getTableList()) {
                if (t.getTableName() != null && t.getTableName().equals(tableName)) {
                    for(ForeignKey fk:t.getForeignKeys().getForeignKeyList()){
                        if(fk.getName()!=null && fk.getName().equals(attributeName)){
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void writeDatabasesInstance(Databases db) throws IOException {
        System.out.println("XML: writing database to file"+Worker.path_to_work + "/" + databasesName);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.writeValue(new File(Worker.path_to_work + "/" + databasesName), db);
    }

    public static void flush(){

        XmlMapper xmlMapper = new XmlMapper();
        try {
            xmlMapper.writeValue(new File(Worker.path_to_work + "/" + databasesName), getDatabasesInstance());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static XML getInstance() {
        if (instance == null) {
            instance = new XML();
        }
        return instance;
    }
}
