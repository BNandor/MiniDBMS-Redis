package persistence;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.Worker;
import struct.Attribute;
import struct.Databases;
import struct.Table;

import java.io.*;
import java.rmi.ServerException;

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
