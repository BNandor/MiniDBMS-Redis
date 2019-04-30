package queries;

import comm.ServerException;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import queries.misc.selectpipeline.IDSource;
import queries.misc.selectpipeline.IndexIDProvider;
import struct.ForeignKey;
import struct.IndexFile;
import struct.Table;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JoinSelectQuery {

    private String query;
    private Map<String, PartialResultNode> nodes;

    public PartialResultNode root;

    public class PartialResultNode extends Thread {

        public ArrayList<PartialResultNode> children;

        public SimpleSelectQuery.PartialResult partialResult;

        private SimpleSelectQuery simpleQuery;
        private ArrayList<String> selectedColumns;
        private String tableName;
        private String simpleQueryString;

        public PartialResultNode() {
            children = new ArrayList<>();
            selectedColumns = new ArrayList<>();
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
                partialResult = simpleQuery.select(simpleQuery.buildQuery());
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
    }

    public JoinSelectQuery(String query) throws comm.ServerException {
        this.query = query;
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
                            PartialResultNode newResultNode = new PartialResultNode();
                            if (root.tableName.equals(leftTableName)) {//if root is being referenced, update root
                                newResultNode.setTableName(rightTableName);
                                nodes.put(rightTableName, newResultNode);
                                ((PartialResultNode) nodes.get(rightTableName)).children.add(root);
                                root = newResultNode;
                            } else {
                                newResultNode.setTableName(leftTableName);
                                nodes.put(leftTableName, newResultNode);
                                ((PartialResultNode) nodes.get(rightTableName)).children.add(newResultNode);
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
                            PartialResultNode newResultNode = new PartialResultNode();
                            if (root.tableName.equals(rightTableName)) {//if root is being referenced, update root
                                newResultNode.setTableName(leftTableName);
                                nodes.put(leftTableName, newResultNode);
                                ((PartialResultNode) nodes.get(leftTableName)).children.add(root);
                                root = newResultNode;
                            } else {
                                newResultNode.setTableName(rightTableName);
                                nodes.put(rightTableName, newResultNode);
                                ((PartialResultNode) nodes.get(leftTableName)).children.add(newResultNode);
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
//        if (!XML.tableExists(query.tableName, Worker.currentlyWorking)) {
//            throw new comm.ServerException("Error: table " + query.tableName + " does not exist");
//        }
//
//        if (query.selectedColumns.size() == 1 && query.selectedColumns.get(0).equals("*")) {//handling wildcard
//            query.selectedColumns.clear();
//            for (Attribute attribute : XML.getTable(query.tableName, Worker.currentlyWorking).getTableStructure().getAttributeList()) {
//                query.selectedColumns.add(attribute.getName());
//            }
//        }
//
//        for (String column : query.selectedColumns) {
//            if (!XML.attributeExists(query.tableName, column, Worker.currentlyWorking)) {
//                throw new comm.ServerException("Error: column " + column + " does not exist now, does it ? ");
//            }
//        }

    }

    private void hashedJoin(PartialResultNode referencer, PartialResultNode referenced) throws ServerException {
        int indexSlot=-1;

        for(ForeignKey fk:XML.getTable(referencer.tableName,Worker.currentlyWorking).getForeignKeys().getForeignKeyList()){
            if(fk.getRefTableName().equals(referenced.tableName)){
                for(IndexFile index:XML.getTable(referencer.tableName,Worker.currentlyWorking).getIndexFiles().getIndexFiles()){
                    if(index.getName().equals(fk.getName())){
                        indexSlot = index.getIndexFileName();
                        break;
                    }
                }
                break;
            }
        }

        if(indexSlot == -1){
            throw new comm.ServerException("Error: there is no connection between "+referencer.tableName+" and "+referenced.tableName);
        }

        Set<Integer> eligibleIds = new TreeSet<>();
        RedisConnector connector = new RedisConnector();
        connector.connect();
        for (Integer referencedKey:referenced.partialResult.getIDs()){
            IDSource idSource = new IndexIDProvider(indexSlot,referencedKey.toString(),connector);
            while(idSource.hasNext()){
                eligibleIds.addAll(idSource.readNext().stream().map(a->Integer.parseInt(a)).collect(Collectors.toList()));
            }
        }

        referencer.partialResult.getIDs().retainAll(eligibleIds);
        connector.closeConnection();
    }

    public void joinAll(PartialResultNode node) throws ServerException {
        if(node.children.isEmpty()){
            return;
        }
        //first join the  children
        for (PartialResultNode child:node.children){
            joinAll(child);
        }
        //then do the join on the results
        for (PartialResultNode child:node.children){
            hashedJoin(node,child);
        }
    }
}
