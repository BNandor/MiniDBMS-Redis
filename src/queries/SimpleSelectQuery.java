package queries;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.ServerException;
import comm.Worker;
import javafx.util.Pair;
import persistence.RedisConnector;
import persistence.XML;
import queries.misc.selectpipeline.FTSIDProvider;
import queries.misc.selectpipeline.IDSource;
import queries.misc.selectpipeline.IndexIDProvider;
import queries.misc.selectresultprotocol.Header;
import queries.misc.selectresultprotocol.Page;
import queries.misc.selectresultprotocol.Row;
import struct.Attribute;
import struct.IndexFile;
import struct.Table;
import struct.Unique;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SimpleSelectQuery {
    private static final int numberOfRowsInPage =   1000;
    private RedisConnector redisConnection;

    class Query {
        public ArrayList<String> selectedColumns;
        public ArrayList<Pair<String, String>> constraints;
        public ArrayList<String> operators;

        public String tableName;

        public Query() {
            selectedColumns = new ArrayList<>();
            constraints = new ArrayList<>();
            operators = new ArrayList<>();
        }

        @Override
        public String toString() {
            return tableName + selectedColumns + "\n" + constraints;
        }
    }

    private PrintWriter messageSender;
    private String queryString;

    public SimpleSelectQuery(String queryString, PrintWriter messageSender) {
        this.messageSender = messageSender;
        this.queryString = queryString;
        this.redisConnection = new RedisConnector();
        redisConnection.connect();
    }
    public SimpleSelectQuery(String queryString) {
        this.queryString = queryString;
        this.redisConnection = new RedisConnector();
        redisConnection.connect();
    }
    public Query buildQuery() throws ServerException {

        String[] splitAtFrom = queryString.split("from");
        if (splitAtFrom.length == 1) {
            splitAtFrom = queryString.split("FROM");
        }
        if (splitAtFrom.length == 1) {
            throw new ServerException("Syntax error in select : " + queryString);
        }
        //Select selected columns
        StringTokenizer selectEliminator = new StringTokenizer(splitAtFrom[0]); // select col1,col2
        selectEliminator.nextToken();//removing first select
        String columns = selectEliminator.nextToken("");//col1,col2
        StringTokenizer columnTokenizer = new StringTokenizer(columns.replaceAll(" ", ""), ",");
        Query query = new Query();
        while (columnTokenizer.hasMoreTokens()) {
            query.selectedColumns.add(columnTokenizer.nextToken());
        }
        String[] splitAtWhere = splitAtFrom[1].split("where");
        if (splitAtWhere.length == 1) {
            splitAtWhere = splitAtFrom[1].split("WHERE");
        }

        if (splitAtWhere.length == 1) {
            query.tableName = splitAtFrom[1].trim();//if there are no constraints
        } else {
            query.tableName = splitAtWhere[0].trim();//TODO implement joins

            String[] constraints = splitAtWhere[1].trim().split("\\s*AND|and+\\s*");//We have yet to support or

            for (int i = 0; i < constraints.length; i++) {
                String constraint = constraints[i];
                String equalityPattern = " *([A-Za-z0-9]+) *= *(.+) *";
                String biggerPattern = " *([A-Za-z0-9]+) *> *(.+) *";
                String smallerPattern = " *([A-Za-z0-9]+) *< *(.+) *";

                Pattern pattern = Pattern.compile(equalityPattern);
                Matcher matcher = pattern.matcher(constraint);

                String column=null;
                String value=null;

                if (matcher.find()) {
                    column = matcher.group(1).trim();
                    value = matcher.group(2).trim();
                    query.operators.add("=");
                }else {
                    pattern = Pattern.compile(biggerPattern);
                    matcher = pattern.matcher(constraint);

                    if (matcher.find()) {
                        column = matcher.group(1).trim();
                        value = matcher.group(2).trim();
                        query.operators.add(">");
                    } else {
                        pattern = Pattern.compile(smallerPattern);
                        matcher = pattern.matcher(constraint);
                        if (matcher.find()) {
                            column = matcher.group(1).trim();
                            value = matcher.group(2).trim();
                            query.operators.add("<");
                        } else {
                            throw new comm.ServerException("Error building query: invalid opertaor in "+constraint);
                        }
                    }
                }

                if (value.charAt(0) == '\'') {
                    value = value.substring(1);
                }

                if (value.charAt(value.length() - 1) == '\'') {
                    value = value.substring(0, value.length() - 1);
                }

                query.constraints.add(new Pair<>(column, value));
            }
        }
        if(!XML.tableExists(query.tableName,Worker.currentlyWorking)){
            throw new comm.ServerException("Error: table "+query.tableName+" does not exist");
        }

        if (query.selectedColumns.size() == 1 && query.selectedColumns.get(0).equals("*")) {//handling wildcard
            query.selectedColumns.clear();
            for (Attribute attribute : XML.getTable(query.tableName, Worker.currentlyWorking).getTableStructure().getAttributeList()) {
                query.selectedColumns.add(attribute.getName());
            }
        }

        for (String column : query.selectedColumns) {
            if (!XML.attributeExists(query.tableName, column, Worker.currentlyWorking)) {
                throw new comm.ServerException("Error: column " + column + " does not exist now, does it ? ");
            }
        }

        return query;
    }

    class PartialResult { //encapsulates a selection from one table only

        private Query query;
        private Table selectedTable;
        private Set<Integer> resultKeys;

        public PartialResult(Query query) {
            this.query = query;
            resultKeys = new TreeSet<>();
        }

        public void add(String index) {
            resultKeys.add(Integer.parseInt(index));
        }

        @Override
        public String toString() {
            return resultKeys + "";
        }
    }

    public void writeResult(PartialResult partialResult) throws ServerException {
        messageSender.write("READY\n");
        messageSender.flush();

        Header header = new Header();
        header.setColumnCount(partialResult.query.selectedColumns.size());
        header.setColumnNames(partialResult.query.selectedColumns);
        int pagenumber = (partialResult.resultKeys.size() % numberOfRowsInPage == 0) ?
                partialResult.resultKeys.size() / numberOfRowsInPage
                :
                partialResult.resultKeys.size() / numberOfRowsInPage + 1;
        header.setPageNumber(pagenumber);
        header.setRowCount(partialResult.resultKeys.size());
        XmlMapper xmlMapper = new XmlMapper();
        try {
            messageSender.write(xmlMapper.writeValueAsString(header) + "\n");
            messageSender.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new comm.ServerException("Error writing header to stream");
        }

        Iterator<Integer> idIterator = partialResult.resultKeys.iterator();
        Worker.RDB.select(partialResult.selectedTable.getSlotNumber());

        for (int i = 0; i < pagenumber; i++) {
            Page page = new Page();
            page.setPageNumber(i);
            for (int j = 0; j < numberOfRowsInPage && idIterator.hasNext(); j++) {
                int currentID = idIterator.next();
                Row row = new Row();
                for (String column : partialResult.query.selectedColumns) {
                    if (column.equals(partialResult.selectedTable.getKey().getName())) {
                        row.getValues().add(String.valueOf(currentID));
                    } else {
                        row.getValues().add(redisConnection.getColumn(currentID + "", column));
                    }
                }
                page.getRows().add(row);
            }
            try {
                messageSender.write(xmlMapper.writeValueAsString(page) + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                throw new comm.ServerException("Error writing page to stream");
            }
            messageSender.flush();
        }
    }

    private boolean thisTablePKSelectable(Query query, String pk) throws ServerException {
        // TODO optimization: do not check constraints on which we have an index
        if (!redisConnection.keyExists(pk)) return false;
        Iterator<String> operatorIterator = query.operators.iterator();
        for (Pair<String, String> p : query.constraints) {
            String realVal = redisConnection.getColumn(pk, p.getKey());
            try {
                switch (operatorIterator.next()) {
                    case "=": {
                        if (realVal == null) {//in this case, the constraint must be inflicted on the pk, because we checked the existence of columns
                            if (!pk.equals(p.getValue())) {
                                return false;
                            }
                            continue;
                        }
                        if (!realVal.equals(p.getValue())) {
                            return false;
                        }
                    }
                    break;
                    case ">": {
                        if (realVal == null) {//in this case, the constraint must be inflicted on the pk, because we checked the existence of columns
                            if (!(Integer.parseInt(pk) > Integer.parseInt(p.getValue()))) {
                                return false;
                            }
                            continue;
                        }
                        if (!(Integer.parseInt(realVal) > Integer.parseInt(p.getValue()))) {
                            return false;
                        }
                    }
                    break;
                    case "<": {
                        if (realVal == null) {//in this case, the constraint must be inflicted on the pk, because we checked the existence of columns
                            if (!(Integer.parseInt(pk) < Integer.parseInt(p.getValue()))) {
                                return false;
                            }
                            continue;
                        }
                        if (!(Integer.parseInt(realVal) < Integer.parseInt(p.getValue()))) {
                            return false;
                        }
                    }
                    break;
                }
            }catch (NumberFormatException ex){
                ex.printStackTrace();
                throw new comm.ServerException("Error, cannot convert string to int in constraints");
            }
        }
        return true;
    }

    private int getUniqueSlot(Table selectedTable, String uniqueAttributeName) throws comm.ServerException {
        for (Unique unique : selectedTable.getUniqeAttributes().getUniqueList()) {
            if (unique.getName().equals(uniqueAttributeName)) {
                for (IndexFile index : selectedTable.getIndexFiles().getIndexFiles()) {
                    if (index.getName().equals(uniqueAttributeName)) {
                        return index.getIndexFileName();
                    }
                }
            }
        }
        throw new comm.ServerException("Internal error, db is in inconsistent state regarding unique slots");
    }

    private int getIndexSlot(Table selectedTable, String indexedAttributeName) throws ServerException {

        for (IndexFile index : selectedTable.getIndexFiles().getIndexFiles()) {
            if (index.getName().equals(indexedAttributeName)) {
                return index.getIndexFileName();
            }
        }
        throw new comm.ServerException("Internal error, db is in inconsistent state regarding index slots");
    }

    public PartialResult select(Query query) throws comm.ServerException {

        System.out.println(query);
        PartialResult result = new PartialResult(query);

        if (!XML.tableExists(query.tableName, Worker.currentlyWorking)) {
            throw new comm.ServerException("Error in select: table " + query.tableName + " does not exist in database " + Worker.currentlyWorking);
        }
        Table selectedTable = XML.getTable(query.tableName, Worker.currentlyWorking);
        result.selectedTable = selectedTable;
        for (Pair<String, String> p : query.constraints) {
            if (!XML.attributeExists(query.tableName, p.getKey(), Worker.currentlyWorking)) {
                throw new comm.ServerException("Error in select: attribute " + p.getKey() + " does not exist");
            }
        }

        //everything checks out, let's select
        Iterator<String> operatorIterator= query.operators.iterator();
        for (Pair<String, String> p : query.constraints){//check for the presence of unique indexes
            String operator = operatorIterator.next();
            if (XML.attributeIsUnique(query.tableName, p.getKey(), Worker.currentlyWorking)) {//we're lucky
                //we have a maximum of one result
                if(operator.equals("=")) {
                    redisConnection.select(getUniqueSlot(selectedTable, p.getKey()));//select index db
                    String uniquePK = redisConnection.get(p.getValue());
                    if (uniquePK == null) {//there is no record with that unique value
                        return result;
                    }
                    redisConnection.select(selectedTable.getSlotNumber());
                    if (thisTablePKSelectable(query, uniquePK)) {
                        result.add(uniquePK);
                    }
                    return result;
                }else{
                    IDSource source = new FTSIDProvider(getUniqueSlot(selectedTable, p.getKey()));
                    Set<String> pres = new TreeSet<>();//TODO implement streaming here also

                    if(operator.equals(">")) {
                        while (source.hasNext()) {
                            for (String uval : source.readNext()) {
                                if (Integer.parseInt(uval) > Integer.parseInt(p.getValue())) {
                                    pres.add(redisConnection.get(uval));
                                }

                            }
                        }
                    }
                    else{
                        while (source.hasNext()) {
                            for (String uval : source.readNext()) {
                                if (Integer.parseInt(uval) < Integer.parseInt(p.getValue())) {
                                    pres.add(redisConnection.get(uval));
                                }
                            }
                        }
                    }
                    redisConnection.select(selectedTable.getSlotNumber());
                    for (String key : pres) {
                        if (thisTablePKSelectable(query, key)) {
                            result.add(key);
                        }
                    }
                        return result;
                }
            }
        }
        operatorIterator= query.operators.iterator();
        for (Pair<String, String> p : query.constraints) {//check for constraints on primary key
            String op = operatorIterator.next();
            if (XML.attributeIsPrimaryKey(query.tableName, p.getKey(), Worker.currentlyWorking) && op.equals("=")) {//we're lucky
                redisConnection.select(selectedTable.getSlotNumber());
                if (thisTablePKSelectable(query, p.getValue())) {
                    result.add(p.getValue());
                }
                return result;
            }
        }

        // at this point, there are no constraints on unique keys,or on primary keys

        ArrayList<Pair<String, String>> equalityIndexed = new ArrayList<>();
        ArrayList<Pair<String, String>> biggerIndexed = new ArrayList<>();
        ArrayList<Pair<String, String>> smallerIndexed = new ArrayList<>();
        operatorIterator = query.operators.iterator();
        for (Pair<String, String> p : query.constraints) {
            String op = operatorIterator.next();
            if (XML.hasIndex(query.tableName, p.getKey(), Worker.currentlyWorking)) {
                switch (op) {
                    case "=":   equalityIndexed.add(new Pair<>(p.getKey(), p.getValue()));break;
                    case ">":   biggerIndexed.add(new Pair<>(p.getKey(), p.getValue()));break;
                    case "<":   smallerIndexed.add(new Pair<>(p.getKey(), p.getValue()));break;
                }
            }
        }

        if (equalityIndexed.size() == 0 && biggerIndexed.size() == 0 && smallerIndexed.size() == 0) {//in this case, there are no indexed columns, initiate FTS
            IDSource ids = new FTSIDProvider(selectedTable.getSlotNumber());
            while (ids.hasNext()) {
                for(String id:ids.readNext()){
                    if(thisTablePKSelectable(query, id)){
                        result.resultKeys.add(Integer.parseInt(id));
                    }
                }
            }
        } else {

            Set<String> indexset = new HashSet<>();
            //set the result of the selection based on the first index file
            IDSource ids;
            if(equalityIndexed.size() > 0) {
                ids = new IndexIDProvider(getIndexSlot(selectedTable, equalityIndexed.get(0).getKey()), equalityIndexed.get(0).getValue());
                while (ids.hasNext()) {
                    indexset.addAll(new ArrayList<>(ids.readNext()));//get everything
                }
                for (int i = 1; i < equalityIndexed.size(); i++) {//for every other indexed Column, perform intersection
                    Set<String> partialindexset = new TreeSet<>();
                    ids = new IndexIDProvider(getIndexSlot(selectedTable, equalityIndexed.get(i).getKey()), equalityIndexed.get(i).getValue());
                    while (ids.hasNext()) {
                        partialindexset.addAll(new ArrayList<>(ids.readNext()));
                    }
                    indexset.retainAll(partialindexset);
                }
            }

            if(biggerIndexed.size()>0){
                for(Pair<String,String> p:biggerIndexed) {
                    IDSource biggerIndexSource = new FTSIDProvider(getIndexSlot(selectedTable, p.getKey()));
                    Set<String> biggerSet = new HashSet<>();
                    while(biggerIndexSource.hasNext()){
                        for(String indexed:biggerIndexSource.readNext()){
                            if(Integer.parseInt(indexed)>Integer.parseInt(p.getValue())){
                                IDSource setSource =new  IndexIDProvider(getIndexSlot(selectedTable, p.getKey()),indexed);
                                while(setSource.hasNext()){
                                    biggerSet.addAll(setSource.readNext());
                                }
                            }
                        }
                    }
                    if(equalityIndexed.size() > 0){
                        indexset.retainAll(biggerSet);
                    }
                    else {
                        indexset.addAll(biggerSet);
                    }
                }
            }
            if(smallerIndexed.size()>0){
                for(Pair<String,String> p:smallerIndexed) {
                    IDSource smallerIndexSource = new FTSIDProvider(getIndexSlot(selectedTable, p.getKey()));
                    Set<String> smallerSet = new HashSet<>();
                    while(smallerIndexSource.hasNext()){
                        for(String indexed:smallerIndexSource.readNext()){
                            if(Integer.parseInt(indexed) < Integer.parseInt(p.getValue())){
                                IDSource setSource =new  IndexIDProvider(getIndexSlot(selectedTable, p.getKey()),indexed);
                                while(setSource.hasNext()){
                                    smallerSet.addAll(setSource.readNext());
                                }
                            }
                        }
                    }
                    if(equalityIndexed.size() > 0 || biggerIndexed.size() > 0 ) {
                        indexset.retainAll(smallerSet);
                    }
                    else {
                        indexset.addAll(smallerSet);
                    }
                }
            }
            redisConnection.select(selectedTable.getSlotNumber());
            for (String key : indexset) {
                if (thisTablePKSelectable(query, key)) {
                    result.add(key);
                }
            }
        }
        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        redisConnection.closeConnection();
    }
}
