package queries;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.ServerException;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import queries.misc.selectpipeline.IDSource;
import queries.misc.selectpipeline.IndexIDProvider;
import queries.misc.selectresultprotocol.Header;
import queries.misc.selectresultprotocol.Page;
import queries.misc.selectresultprotocol.Row;
import struct.Attribute;
import struct.ForeignKey;
import struct.IndexFile;
import struct.Table;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JoinSelectQuery {

    private String query;
    private Map<String, PartialResultNode> nodes;

    public PartialResultNode getRoot() {
        return root;
    }

    public PartialResultNode root;
    private static final int numberOfRowsInPage = SimpleSelectQuery.numberOfRowsInPage;

    public class PartialResultNode extends Thread {

        public ArrayList<PartialResultNode> children;
        public ArrayList<String> connectingColumnNames;
        public SimpleSelectQuery.PartialResult partialResult;

        protected SimpleSelectQuery simpleQuery;
        private ArrayList<String> selectedColumns;
        protected String tableName;
        private String simpleQueryString;
        private Map<Integer, ArrayList<String>> rows;

        public PartialResultNode() {
            children = new ArrayList<>();
            selectedColumns = new ArrayList<>();
            connectingColumnNames = new ArrayList<>();
            rows = new HashMap<>();
        }

        public PartialResultNode instantiate() {
            return new PartialResultNode();
        }

        @Override
        public String toString() {
            return tableName + " -> ( " + children + " ) \n" + simpleQueryString;
        }

        public void concatConstraint(String constraint) throws ServerException {
            if (!simpleQueryString.contains("where")) {
                simpleQueryString += " where " + constraint;
            } else {
                simpleQueryString += " AND " + constraint;
            }
        }

        public void initPartialResult(SimpleSelectQuery.Query query) {
            partialResult = simpleQuery.new PartialResult(query);
        }

        public String inOrder() {
            String result = "";
            for (PartialResultNode node : children) {
                result += node.inOrder() + tableName + " ";
            }
            if (children.size() == 0) {
                result = tableName + " ";
            }
            return result;
        }

        @Override
        public void run() {
            super.run();
            simpleQuery = new SimpleSelectQuery(simpleQueryString);
            try {
                SimpleSelectQuery.Query query = simpleQuery.buildQuery();
                initPartialResult(query);
                simpleQuery.select(query, partialResult);
                //System.out.println(simpleQueryString + "->" + partialResult);
            } catch (comm.ServerException e) {
                e.printStackTrace();
                simpleQuery.setErrorMessage(e.getMessage());
                System.out.println(simpleQueryString);
            }
        }

        public void setTableName(String leftTableName) {
            tableName = leftTableName;
            simpleQueryString = "select * from " + tableName;
        }
    }

    public class HashedPartialResultNode extends PartialResultNode {
        @Override
        public void initPartialResult(SimpleSelectQuery.Query query) {
            partialResult = simpleQuery.new HashedPartialResult(query, connectingColumnNames);
        }

        @Override
        public PartialResultNode instantiate() {
            return new HashedPartialResultNode();
        }
    }

    public void runSubqueries() throws ServerException {
        for (String table : nodes.keySet()) {
            nodes.get(table).start();
        }

        for (String table : nodes.keySet()) {
            try {
                nodes.get(table).join();
                if (nodes.get(table).simpleQuery.getErrorMessage() != null) {
                    throw new comm.ServerException(nodes.get(table).simpleQuery.getErrorMessage());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Finished running subqueries");
    }

    public JoinSelectQuery(String query) throws comm.ServerException {
        this.query = query;
        //root = new HashedPartialResultNode();
        root = new PartialResultNode();
        nodes = new HashMap<>();
        buildGraph(query);
    }

    public void buildGraph(String query) throws comm.ServerException {

        String[] splitAtFrom = query.split("from");
        if (splitAtFrom.length == 1) {
            splitAtFrom = query.split("FROM");
        }
        if (splitAtFrom.length == 1) {
            throw new ServerException("Syntax error in select : " + query);
        }
        //Select selected columns
        StringTokenizer selectEliminator = new StringTokenizer(splitAtFrom[0]); // select col1,col2
        selectEliminator.nextToken();//removing first select
        String columns = selectEliminator.nextToken("");//col1,col2
        StringTokenizer columnTokenizer = new StringTokenizer(columns.replaceAll(" ", ""), ",");

        ArrayList<String> selectedColumns = new ArrayList<>();
        while (columnTokenizer.hasMoreTokens()) {
            selectedColumns.add(columnTokenizer.nextToken());
        }


        String[] splitAtWhere = splitAtFrom[1].split("where");
        if (splitAtWhere.length == 1) {
            splitAtWhere = splitAtFrom[1].split("WHERE");
        }

        String[] joins = null;
        if (splitAtWhere.length > 1) {
            joins = splitAtWhere[0].split("\\s*JOIN|join+\\s*");
        } else {
            //if there are no constraints
            joins = splitAtFrom[1].split("\\s*JOIN|join+\\s*");
        }

        root.setTableName(joins[0].trim());
        if (!XML.tableExists(root.tableName, Worker.currentlyWorking)) {
            throw new comm.ServerException("Error, table " + root.tableName + "does not exist");
        }
        nodes.put(root.tableName, root);

        for (int i = 1; i < joins.length; i++) {
            String[] onSplit = joins[i].split("\\s*ON|on+\\s*"); // B ON A.FID = B.ID

            String[] equiSplit = onSplit[1].split("\\s*=|=+\\s*"); // A.FID = B.ID
            if (equiSplit.length != 2) throw new comm.ServerException("Syntax error 1 near " + onSplit[0]);
            if (equiSplit[0].split("\\.").length != 2 || equiSplit[1].split("\\.").length != 2) {
                throw new comm.ServerException("Syntax error 2 near " + equiSplit[1]);
            }

            String leftTableName = equiSplit[0].split("\\.")[0].trim(); //A
            String leftColumnName = equiSplit[0].split("\\.")[1].trim();//FID

            String rightTableName = equiSplit[1].split("\\.")[0].trim(); //B
            String rightColumnName = equiSplit[1].split("\\.")[1].trim();//ID

            if (!XML.tableExists(leftTableName, Worker.currentlyWorking)) {
                throw new comm.ServerException("Error, table " + leftTableName + " does not exist");
            }

            if (!XML.tableExists(rightTableName, Worker.currentlyWorking)) {
                throw new comm.ServerException("Error, table " + rightTableName + " does not exist");
            }
            // A,B are existing tables

            if (!XML.attributeExists(leftTableName, leftColumnName, Worker.currentlyWorking)) {
                throw new comm.ServerException("Error, column " + leftColumnName + " does not exist");
            }
            if (!XML.attributeExists(rightTableName, rightColumnName, Worker.currentlyWorking)) {
                throw new comm.ServerException("Error, column " + rightColumnName + " does not exist");
            }
            // A.FID, B.ID are existing columns

            Table leftTable = XML.getTable(leftTableName, Worker.currentlyWorking);
            Table rightTable = XML.getTable(rightTableName, Worker.currentlyWorking);

            if (leftTable.getKey().getName().equals(leftColumnName)) {
                if (leftTable.getKey().getName().equals(leftColumnName)) {
                    boolean fkSet = false;
                    for (ForeignKey fk : rightTable.getForeignKeys().getForeignKeyList()) {
                        if (fk.getName().equals(rightColumnName)) {
                            if (!fk.getRefTableName().equals(leftTableName) || !fk.getRefTableAttributeName().equals(leftColumnName)) {
                                throw new comm.ServerException("Error 1, wrong foreign key given in " + onSplit[1]);
                            }
                            //Everything is fine
                            //rightTableName.rightColumnName references leftTableName.leftColumnName
                            PartialResultNode newResultNode = root.instantiate();
                            if (root.tableName.equals(leftTableName)) {//if root is being referenced, update root
                                newResultNode.setTableName(rightTableName);
                                nodes.put(rightTableName, newResultNode);
                                (nodes.get(rightTableName)).children.add(root);
                                (nodes.get(rightTableName)).connectingColumnNames.add(rightColumnName);
                                root = newResultNode;
                            } else {
                                newResultNode.setTableName(leftTableName);
                                nodes.put(leftTableName, newResultNode);
                                (nodes.get(rightTableName)).children.add(newResultNode);
                                (nodes.get(rightTableName)).connectingColumnNames.add(rightColumnName);
                            }
                            fkSet = true;
                            break;
                        }
                    }
                    if (!fkSet) {
                        throw new comm.ServerException("Error 2, wrong foreign key given in " + onSplit[1]);
                    }
                } else {
                    throw new comm.ServerException("Error 3, wrong id given in " + onSplit[1]);
                }
            } else {
                if (rightTable.getKey().getName().equals(rightColumnName)) {
                    boolean fkSet = false;
                    for (ForeignKey fk : leftTable.getForeignKeys().getForeignKeyList()) {

                        if (fk.getName().equals(leftColumnName)) {
                            if (!fk.getRefTableName().equals(rightTableName) || !fk.getRefTableAttributeName().equals(rightColumnName)) {
                                throw new comm.ServerException("Error 1, wrong foreign key given in " + onSplit[1]);
                            }
                            //Everything is fine
                            //leftTableName.leftColumnName references rightTableName.rightColumnName
                            PartialResultNode newResultNode = root.instantiate();
                            if (root.tableName.equals(rightTableName)) {//if root is being referenced, update root
                                newResultNode.setTableName(leftTableName);
                                nodes.put(leftTableName, newResultNode);
                                ((PartialResultNode) nodes.get(leftTableName)).children.add(root);
                                (nodes.get(leftTableName)).connectingColumnNames.add(leftColumnName);
                                root = newResultNode;
                            } else {
                                newResultNode.setTableName(rightTableName);
                                nodes.put(rightTableName, newResultNode);
                                ((PartialResultNode) nodes.get(leftTableName)).children.add(newResultNode);
                                (nodes.get(leftTableName)).connectingColumnNames.add(leftColumnName);
                            }


                            fkSet = true;
                            break;
                        }
                    }
                    if (!fkSet) {
                        throw new comm.ServerException("Error 2, wrong foreign key given in " + onSplit[1]);
                    }
                } else {
                    throw new comm.ServerException("Error 3, wrong id given in " + onSplit[1]);
                }
            }
        }

        if (splitAtWhere.length > 1) {
            //  query.tableName = splitAtWhere[0].trim();//TODO implement joins

            String[] constraints = splitAtWhere[1].trim().split("\\s*AND|and+\\s*");//We have yet to support or

            for (int i = 0; i < constraints.length; i++) {
                String constraint = constraints[i];
                String equalityPattern = " *([A-Za-z0-9.]+) *= *(.+) *";
                String biggerPattern = " *([A-Za-z0-9.]+) *> *(.+) *";
                String smallerPattern = " *([A-Za-z0-9.]+) *< *(.+) *";

                Pattern pattern = Pattern.compile(equalityPattern);
                Matcher matcher = pattern.matcher(constraint);

                String column = null;
                String value = null;
                String operator = null;
                if (matcher.find()) {
                    column = matcher.group(1).trim();
                    value = matcher.group(2).trim();
                    operator = "=";
                } else {
                    pattern = Pattern.compile(biggerPattern);
                    matcher = pattern.matcher(constraint);

                    if (matcher.find()) {
                        column = matcher.group(1).trim();
                        value = matcher.group(2).trim();
                        operator = ">";
                    } else {
                        pattern = Pattern.compile(smallerPattern);
                        matcher = pattern.matcher(constraint);
                        if (matcher.find()) {
                            column = matcher.group(1).trim();
                            value = matcher.group(2).trim();
                            operator = "<";
                        } else {
                            throw new comm.ServerException("Error building query: invalid operator in " + constraint);
                        }
                    }
                }

                if (column.split("\\.").length != 2) {
                    throw new comm.ServerException("Syntax error 3: near " + column + " please absolute Column name in joint select");
                }

                String consTable = column.split("\\.")[0];
                String consColmun = column.split("\\.")[1];
                nodes.get(consTable).concatConstraint(consColmun + " " + operator + " " + value);
            }
        }

        //Handle projection
        if (selectedColumns.size() == 1 && selectedColumns.get(0).equals("*")) {
            for (String tableName : nodes.keySet()) {
                for (Attribute attr : XML.getTable(tableName, Worker.currentlyWorking).getTableStructure().getAttributeList()) {
                    nodes.get(tableName).selectedColumns.add(attr.getName());
                }
            }
        } else {
            for (String column : selectedColumns) {
                if (column.split("\\.").length != 2) {
                    throw new comm.ServerException("Error in column " + column + " please use absolute column name");
                }
                String tableName = column.split("\\.")[0];
                String columnName = column.split("\\.")[1];

                if (nodes.get(tableName) == null) {
                    throw new comm.ServerException("Error in column " + column + " invalid table");
                }
                if (!XML.attributeExists(tableName, columnName, Worker.currentlyWorking)) {
                    throw new comm.ServerException("Error in column " + column + " invalid column name");
                }
                nodes.get(tableName).selectedColumns.add(columnName);
            }
        }
    }

    private void indexedJoin(PartialResultNode referencer, PartialResultNode referenced) throws ServerException {
        int indexSlot = -1;

        for (ForeignKey fk : XML.getTable(referencer.tableName, Worker.currentlyWorking).getForeignKeys().getForeignKeyList()) {
            if (fk.getRefTableName().equals(referenced.tableName)) {
                for (IndexFile index : XML.getTable(referencer.tableName, Worker.currentlyWorking).getIndexFiles().getIndexFiles()) {
                    if (index.getName().equals(fk.getName())) {
                        indexSlot = index.getIndexFileName();
                        break;
                    }
                }
                break;
            }
        }

        if (indexSlot == -1) {
            throw new comm.ServerException("Error: there is no connection between " + referencer.tableName + " and " + referenced.tableName);
        }

        Set<Integer> eligibleIds = new TreeSet<>();
        RedisConnector connector = new RedisConnector();
        connector.connect();
        for (Integer referencedKey : referenced.partialResult.getIDs()) {
            IDSource idSource = new IndexIDProvider(indexSlot, referencedKey.toString(), connector);
            while (idSource.hasNext()) {
                eligibleIds.addAll(idSource.readNext().stream().map(a -> Integer.parseInt(a)).collect(Collectors.toList()));
            }
        }

        referencer.partialResult.getIDs().retainAll(eligibleIds);
        connector.closeConnection();
        System.out.println("Done joining " + referencer.tableName + " AND " + referenced.tableName);
    }

    public void indexedjoinAll(PartialResultNode node) throws ServerException {
        if (node.children.isEmpty()) {
            return;
        }
        //first join the  children
        for (PartialResultNode child : node.children) {
            indexedjoinAll(child);
        }
        //then do the join on the results
        for (PartialResultNode child : node.children) {
            indexedJoin(node, child);
        }
    }

    private void hashedJoin(PartialResultNode referencer, PartialResultNode referenced) throws comm.ServerException {
        String connectingColumn = null;
        Table referencerTable = XML.getTable(referencer.tableName, Worker.currentlyWorking);
        for (ForeignKey fk : referencerTable.getForeignKeys().getForeignKeyList()) {
            if (fk.getRefTableName().equals(referenced.tableName)) {
                connectingColumn = fk.getName();
                break;
            }
        }


        RedisConnector connector = new RedisConnector();
        connector.connect();

        for (Integer referencedKey : referenced.partialResult.getIDs()) {
            IDSource idSource = new IndexIDProvider(
                    referencerTable.getSlotNumber(),
                    referencer.partialResult.hashFunction(connectingColumn, String.valueOf(referencedKey)),
                    referencedKey + "@*", connector);
            while (idSource.hasNext()) {
                for (String eligiblekey : idSource.readNext().stream().map(b -> b.split("@")[1]).collect(Collectors.toCollection(ArrayList::new))) {
                    connector.addToSet("eligible", eligiblekey);
                    //System.out.println("Adding eligble for "+referencer.tableName+" "+eligiblekey);
                }
            }
        }
        System.out.println("set size before intersection" + connector.getSizeOfSet("selectionresult"));
        Set<String> intersection = connector.setIntersect("eligible", "selectionresult");
        connector.delkey("eligible");
        connector.delkey("selectionresult");

        for (String key : intersection) {
            connector.addToSet("selectionresult", key);
        }
        System.out.println("set size after intersection" + connector.getSizeOfSet("selectionresult"));

        connector.select(XML.getTable(referenced.tableName, Worker.currentlyWorking).getSlotNumber());
        connector.delkey("selectionresult");
        System.out.println("Done joining " + referencer.tableName + " AND " + referenced.tableName);

        connector.closeConnection();
    }

    public void cleanFinalResult() throws comm.ServerException {
        RedisConnector connector = new RedisConnector();
        connector.connect();
        connector.select(XML.getTable(root.tableName, Worker.currentlyWorking).getSlotNumber());
        connector.delkey("selectionresult");
        connector.closeConnection();
    }

    public void hashedjoinAll(PartialResultNode node) throws ServerException {
        if (node.children.isEmpty()) {
            node.partialResult.cleanupHashSets();

            return;
        }
        //first join the  children
        for (PartialResultNode child : node.children) {
            hashedjoinAll(child);
        }
        //then do the join on the results
        for (PartialResultNode child : node.children) {
            hashedJoin(node, child);
        }
        node.partialResult.cleanupHashSets();
    }

    public ArrayList<String> getColumns(PartialResultNode node) {

        ArrayList<String> result = node.selectedColumns;
        result = result.stream().map(col -> node.tableName + "." + col).collect(Collectors.toCollection(ArrayList::new));
        for (PartialResultNode child : node.children) {
            result.addAll(getColumns(child));
        }
        return result;
    }

    public void setConnectorToDefaultTable(PartialResultNode node) {
        node.simpleQuery.getRedisConnection().select(node.partialResult.getSelectedTable().getSlotNumber());
        for (PartialResultNode child : node.children) {
            setConnectorToDefaultTable(child);
        }
    }

    private ArrayList<String> getNodeValues(Integer pk, PartialResultNode node) {
        if (node.rows.containsKey(pk))
            return node.rows.get(pk);
        ArrayList<String> result = new ArrayList<>();

        for (String column : node.selectedColumns) {
            if (node.partialResult.getSelectedTable().getKey().getName().equals(column)) {
                result.add(pk + "");
            } else {
                result.add(node.simpleQuery.getRedisConnection().getColumn(pk.toString(), column));
            }
        }
        node.rows.put(pk, result);
        return result;
    }

    public void appendToRow(Integer pk, PartialResultNode node, Row row) {

        row.getValues().addAll(getNodeValues(pk, node));
        Iterator<String> fkColumnNameIterator = node.connectingColumnNames.iterator();
        for (PartialResultNode child : node.children) {
            Integer referencedId = Integer.parseInt(node.simpleQuery.getRedisConnection().getColumn(pk.toString(), fkColumnNameIterator.next()));
            appendToRow(referencedId, child, row);
        }
    }

    public void writeResult(PrintWriter messageSender) throws ServerException {

        messageSender.write("READY\n");
        messageSender.flush();
        Header header = new Header();
        ArrayList<String> columns = getColumns(root);
        header.setColumnCount(columns.size());
        header.setColumnNames(columns);
        header.setRowCount(root.partialResult.getIDs().size());
        int pageNumber = (root.partialResult.getIDs().size() % numberOfRowsInPage == 0) ?
                root.partialResult.getIDs().size() / numberOfRowsInPage
                :
                root.partialResult.getIDs().size() / numberOfRowsInPage + 1;
        header.setPageNumber(pageNumber);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            messageSender.write(xmlMapper.writeValueAsString(header) + "\n");
            messageSender.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new comm.ServerException("Error writing header to stream");
        }

        //get values page by page

        Iterator<Integer> idIterator = root.partialResult.getIDs().iterator();
        setConnectorToDefaultTable(root);//set every table's connection, to the corresponding table

        for (int i = 0; i < pageNumber; i++) {
            Page page = new Page();
            page.setPageNumber(i);
            for (int j = 0; j < numberOfRowsInPage && idIterator.hasNext(); j++) {
                int currentID = idIterator.next();
                Row row = new Row();
                appendToRow(currentID, root, row);
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
}
