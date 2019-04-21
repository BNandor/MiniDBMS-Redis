package queries;
import comm.ServerException;
import comm.Worker;
import javafx.util.Pair;
import persistence.XML;
import struct.IndexFile;
import struct.Table;
import struct.Unique;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class SelectQuery {

    class Query{
        public ArrayList<String> selectedColumns;
        public ArrayList<Pair<String,String>> constraints;
        public String tableName;

        public Query() {
            selectedColumns = new ArrayList<>();
            constraints = new ArrayList<>();
        }

        @Override
        public String toString() {
            return tableName+selectedColumns+"\n"+constraints;
        }
    }

    private PrintWriter messageSender;
    private String queryString;

    public SelectQuery(String queryString, PrintWriter messageSender) {
        this.messageSender = messageSender;
        this.queryString = queryString;
    }

    public Query buildQuery() throws ServerException {

        String[] splitatFrom = queryString.split("from");
        if (splitatFrom.length == 1){
            splitatFrom = queryString.split("FROM");
        }
        if(splitatFrom.length ==1){
            throw new ServerException("Syntax error in select : "+queryString);
        }
        //Select selectted columns
        StringTokenizer selectEliminator = new StringTokenizer(splitatFrom[0]); // select col1,col2
        selectEliminator.nextToken();//removing first select
        String columns = selectEliminator.nextToken("");//col1,col2
        StringTokenizer columnTokenizer = new StringTokenizer(columns.replaceAll(" ",""),",");
        Query query = new Query();
        while(columnTokenizer.hasMoreTokens()){
            query.selectedColumns.add(columnTokenizer.nextToken());
        }
        String[] splitatWhere = splitatFrom[1].split("where");
        if(splitatWhere.length == 1){
            splitatWhere = splitatFrom[1].split("WHERE");
        }

        if(splitatWhere.length == 1){
            query.tableName = splitatFrom[1].trim();//if there are no constraints
        }else{
            query.tableName = splitatWhere[0].trim();//TODO implement joins

            String[] constraints = splitatWhere[1].trim().split("\\s*AND|and+\\s*");//We have yet to support or

            for (int i=0;i<constraints.length;i++){
                String constraint = constraints[i];
                String[] constraintelements = constraint.split("=");
                String column = constraintelements[0].trim();
                String value = constraintelements[1].trim();

                if(value.charAt(0) == '\'' ){
                    value = value.substring(1);
                }

                if(value.charAt(value.length()-1) == '\''){
                    value = value.substring(0,value.length()-1);
                }

                query.constraints.add(new Pair<>(column,value));
            }
        }


        return query;
    }

    class PartialResult{
        private Set<Integer> resultKeys;

        public PartialResult() {
            resultKeys = new TreeSet<>();
        }

        public void add(Integer index){
            resultKeys.add(index);
        }

        @Override
        public String toString() {
            return resultKeys+"";
        }
    }

    private boolean thisTablePKSelectable(Query query, String pk){
        if(!Worker.RDB.keyExists(pk))return false;
        for (Pair<String,String> p:query.constraints) {
            String realVal = Worker.RDB.getColumn(pk,p.getKey());
            if(realVal == null){//in this case, the constraint must be inflicted on the pk, because we checked the existence of columns
                if(!pk.equals(p.getValue())) {
                    return false;
                }
                continue;
            }
            if(!realVal.equals(p.getValue())) {
                return false;
            }
        }
        return true;
    }

    private int getUniqueSlot(Table selectedTable,String uniqueAttributeName) throws ServerException {
        for (Unique unique:selectedTable.getUniqeAttributes().getUniqueList()){
            if(unique.getName().equals(uniqueAttributeName)){
                for (IndexFile index:selectedTable.getIndexFiles().getIndexFiles()){
                    if(index.getName().equals(uniqueAttributeName)){
                        return index.getIndexFileName();
                    }
                }
            }
        }
        throw new comm.ServerException("Internal error, db is in inconsistent state regarding unique slots");
    }
    public PartialResult select(Query query) throws ServerException {
        System.out.println(query);
        PartialResult result = new PartialResult();

        if(!XML.tableExists(query.tableName, Worker.currentlyWorking)){
            throw new comm.ServerException("Error in select: table "+query.tableName+" does not exist in database "+Worker.currentlyWorking);
        }
        Table selectedTable = XML.getTable(query.tableName,Worker.currentlyWorking);

        for (Pair<String,String> p:query.constraints){
            if(!XML.attributeExists(query.tableName,p.getKey(),Worker.currentlyWorking)){
                throw new comm.ServerException("Error in select: attribute "+p.getKey()+" does not exist");
            }
        }

        //everything checks out, let's select

        for (Pair<String,String> p:query.constraints){//check for the presence of unique indexes
            if(XML.attributeIsUnique(query.tableName,p.getKey(),Worker.currentlyWorking)) {//we're lucky
                                                                                            //we have a maximum of one result
                Worker.RDB.select(getUniqueSlot(selectedTable,p.getKey()));//select index db
                String uniquePK = Worker.RDB.get(p.getValue());
                if(uniquePK == null){//there is no record with that unique value
                    return result;
                }
                Worker.RDB.select(selectedTable.getSlotNumber());
                if(thisTablePKSelectable(query,uniquePK)){
                    result.add(Integer.parseInt(uniquePK));
                }
                return result;
            }
        }

        for (Pair<String,String> p:query.constraints){//check for constraints on primary key
            if(XML.attributeIsPrimaryKey(query.tableName,p.getKey(),Worker.currentlyWorking)){//we're lucky
                Worker.RDB.select(selectedTable.getSlotNumber());
                if(thisTablePKSelectable(query,p.getValue())){
                    result.add(Integer.parseInt(p.getValue()));
                }
                return result;
            }
        }
        return result;
    }
}
