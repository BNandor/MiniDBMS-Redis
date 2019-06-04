package queries;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import comm.ServerException;
import comm.Worker;
import persistence.RedisConnector;
import persistence.XML;
import queries.misc.selectpipeline.FTSIDProvider;
import queries.misc.selectpipeline.IndexIDProvider;
import queries.misc.selectresultprotocol.Header;
import queries.misc.selectresultprotocol.Page;
import queries.misc.selectresultprotocol.Row;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupByQuery {
    private static final int numberOfRowsInPage = SimpleSelectQuery.numberOfRowsInPage;
    private static String[] operationsList = {"AVG", "SUM", "MIN", "MAX"};
    private String queryToParse;
    private String selectAllQuery;
    private String groupingColumn;
    private boolean invalidColumn;
    private HashMap<String, Integer> resultColumnMap;
    private ArrayList<accumRelationship> havingConditions;


    JoinSelectQuery joinSelectQuery;
    SimpleSelectQuery simpleSelectQuery;
    Iterator<Integer> idIterator;
    Page allPage;
    Page groupingPage;

    class accumRelationship {
        public accumRelationship(String column, String operator, Double value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
        }

        private String column;
        private String operator;
        private Double value;

        @Override
        public String toString() {
            return column + operator + value + " ";
        }
    }

    private ArrayList<String> selectedColumns;
    ArrayList<String> resultColumns;

    public GroupByQuery(String queryToParse) throws ServerException {
        this.queryToParse = queryToParse;
        selectedColumns = new ArrayList<>();
        resultColumns = new ArrayList<>();
        havingConditions = new ArrayList<>();
        allPage = new Page();
        groupingPage = new Page();
        resultColumnMap = new HashMap<>();
        extractColumnsToGroupBy(queryToParse);
        System.out.println("Grouping column" + groupingColumn);
        System.out.println("selected Columns" + selectedColumns);
        System.out.println(havingConditions);
        System.out.println("New query " + selectAllQuery);

         selection(selectAllQuery);

        System.out.println("Finished selection");
        //printPage(allPage);
        // sortByGroupingColumn();

         makeGroupingPage();

    }

    public void writeResult(PrintWriter messageSender) throws ServerException {
        messageSender.write("READY\n");
        messageSender.flush();
        Header header = new Header();
        ArrayList<String> columns = new ArrayList<>();
        columns.add(groupingColumn);
        for (String operatingColumn : selectedColumns) {
            if (!operatingColumn.equals(groupingColumn)) {
                columns.add(operatingColumn);
            }
        }
        header.setColumnCount(columns.size());
        header.setColumnNames(columns);
        header.setRowCount(groupingPage.getRows().size());
        int pageNumber = (groupingPage.getRows().size() % numberOfRowsInPage == 0) ?
                groupingPage.getRows().size() / numberOfRowsInPage
                :
                groupingPage.getRows().size() / numberOfRowsInPage + 1;
        header.setPageNumber(pageNumber);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            messageSender.write(xmlMapper.writeValueAsString(header) + "\n");
            messageSender.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new comm.ServerException("Error writing header to stream");
        }
        int currentGroupPageindex = 0;
        for (int i = 0; i < pageNumber; i++) {
            Page page = new Page();
            page.setPageNumber(i);
            for (int j = 0; j < numberOfRowsInPage && currentGroupPageindex < groupingPage.getRows().size(); j++) {
                page.getRows().add(groupingPage.getRows().get(currentGroupPageindex));
                currentGroupPageindex++;
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

    private double operation(Page subpage, String operationColumn) throws ServerException {
        String op = operationColumn.split("\\(")[0];
        String concreteColumn = operationColumn;
        switch (op) {
            case "AVG": {
                double sum = 0;
                for (Row row : subpage.getRows()) {
                    sum += Double.parseDouble(row.getValues().get(getColumnIndex(concreteColumn)));
                }
                return sum / subpage.getRows().size();
            }

            case "SUM": {
                double sum = 0;
                for (Row row : subpage.getRows()) {
                    sum += Double.parseDouble(row.getValues().get(getColumnIndex(concreteColumn)));
                }
                return sum;
            }
            case "MIN": {

                double min = Double.parseDouble(subpage.getRows().get(0).getValues().get(getColumnIndex(concreteColumn)));

                for (int i = 1; i < subpage.getRows().size(); ++i) {
                    double rowval = Double.parseDouble(subpage.getRows().get(i).getValues().get(getColumnIndex(concreteColumn)));
                    if (min > rowval) {
                        min = rowval;
                    }
                }
                return min;
            }
            case "MAX": {
                double max = Double.parseDouble(subpage.getRows().get(0).getValues().get(getColumnIndex(concreteColumn)));

                for (int i = 1; i < subpage.getRows().size(); ++i) {
                    double rowval = Double.parseDouble(subpage.getRows().get(i).getValues().get(getColumnIndex(concreteColumn)));
                    if (max < rowval) {
                        max = rowval;
                    }
                }
                return max;
            }
        }
        return 0;
    }
    private boolean havingOk(Double havingvalue,Double currentValue,String op) throws ServerException {
        switch (op){
            case "=":return havingvalue == currentValue;
            case ">":return havingvalue < currentValue;
            case "<":return havingvalue > currentValue;
        }
        throw new comm.ServerException("Invalid operation"+op);
    }
    private accumRelationship getColumnConditionInHaving(ArrayList<accumRelationship> resultConditions,String column){
        for (accumRelationship rel:resultConditions){
            if(rel.column.equals(column)){
                return rel;
            }
            System.out.println(rel.column+"!="+column);
        }
        return null;
    }
    private void makeGroupingPage() throws ServerException {
        int currentindex = 0;

        while (currentindex < allPage.getRows().size()) {
            Page subPage = new Page();
            subPage.getRows().add(allPage.getRows().get(currentindex));
            while (currentindex < allPage.getRows().size() - 1 && allPage.getRows().get(currentindex).getValues().get(getColumnIndex(groupingColumn)).equals(
                    allPage.getRows().get(currentindex + 1).getValues().get(getColumnIndex(groupingColumn)))
            ) {
                subPage.getRows().add(allPage.getRows().get(currentindex + 1));
                ++currentindex;
            }

            Row resultRow = new Row();
            resultRow.getValues().add(subPage.getRows().get(0).getValues().get(getColumnIndex(groupingColumn)));
            boolean havingClear=true;
            for (String operatingColumn : selectedColumns) {
                if (!operatingColumn.equals(groupingColumn)) {
                    double operationResult = operation(subPage, operatingColumn);
                    accumRelationship relationship = getColumnConditionInHaving(havingConditions,operatingColumn);

                    if(relationship==null || havingOk(relationship.value,operationResult,relationship.operator)) {
                        resultRow.getValues().add(operationResult + "");
                    }else{
                        havingClear=false;
                        break;
                    }
                }
            }
            if(havingClear) {
                groupingPage.getRows().add(resultRow);
            }
            ++currentindex;
        }
    }

    private void printPage(Page page) {
        for (Row row : page.getRows()) {
            System.out.println(row.getValues());
        }
    }

    private void sortByGroupingColumn() throws ServerException {
        initResultColumnMap();
        invalidColumn = false;
        allPage.getRows().sort(new RowComparator());
        if (invalidColumn) {
            throw new comm.ServerException("invalidcolumn detected " + groupingColumn);
        }
    }

    class RowComparator implements Comparator<Row> {

        @Override
        public int compare(Row row1, Row row2) {
            try {
                try {
                    if (Double.parseDouble(row1.getValues().get(getColumnIndex(groupingColumn))) <
                            Double.parseDouble(row2.getValues().get(getColumnIndex(groupingColumn)))) {
                        return -1;
                    } else {
                        if (Double.parseDouble(row1.getValues().get(getColumnIndex(groupingColumn))) ==
                                Double.parseDouble(row2.getValues().get(getColumnIndex(groupingColumn)))) {
                            return 0;
                        }
                    }
                    return 1;
                } catch (NumberFormatException ex) {
                    return row1.getValues().get(getColumnIndex(groupingColumn)).
                            compareTo(
                                    row2.getValues().get(getColumnIndex(groupingColumn)));
                }
            } catch (ServerException e) {
                invalidColumn = false;
            }
            return -1;
        }
    }

    private void selection(String query) throws ServerException {
        try {
            if (query.contains("JOIN") || query.contains("join")) {
                joinSelectQuery = new JoinSelectQuery(query);

                joinSelectQuery.runSubqueries();
                joinSelectQuery.indexedjoinAll(joinSelectQuery.root);

                //joinSelectQuery.hashedjoinAll(joinSelectQuery.root);
                joinSelectQuery.cleanFinalResult();

                //System.out.println(joinSelectQuery.root.partialResult);
                idIterator = joinSelectQuery.getRoot().partialResult.getIDs().iterator();
                joinSelectQuery.setConnectorToDefaultTable(joinSelectQuery.getRoot());

                int id;
                resultColumns = joinSelectQuery.getColumns(joinSelectQuery.getRoot());
                while (idIterator.hasNext()) {
                    id = idIterator.next();
                    Row row = new Row();
                    joinSelectQuery.appendToRow(id, joinSelectQuery.getRoot(), row);
                    allPage.getRows().add(row);
                }
                sortByGroupingColumn();
            } else {

                simpleSelectQuery = new SimpleSelectQuery(query);
                SimpleSelectQuery.Query parsedquery = simpleSelectQuery.buildQuery();
                SimpleSelectQuery.PartialResult result = simpleSelectQuery.new PartialResult(parsedquery);

                if (XML.hasIndex(parsedquery.tableName, groupingColumn, Worker.currentlyWorking)
                        && (!query.contains("WHERE") && !query.contains("where"))) {//we can apply indexing
                    resultColumns = result.getQuery().selectedColumns;

                    RedisConnector pkconnection = new RedisConnector();
                    pkconnection.connect();
                    RedisConnector pkidconnection = new RedisConnector();
                    pkidconnection.connect();
                    RedisConnector simpleconnection = new RedisConnector();
                    simpleconnection.connect();

                    simpleconnection.select(XML.getTable(parsedquery.tableName,Worker.currentlyWorking).getSlotNumber());

                    int indexSlot = simpleSelectQuery.getIndexSlot(XML.getTable(parsedquery.tableName,Worker.currentlyWorking),groupingColumn);
                    FTSIDProvider indexPkProvider = new FTSIDProvider(indexSlot,pkconnection,false);
                    while(indexPkProvider.hasNext()){//for all values

                        for(String groupColumnValue:indexPkProvider.readNext()) {
                            IndexIDProvider indexIDProvider = new IndexIDProvider(indexSlot,groupColumnValue,pkidconnection);
                            while(indexIDProvider.hasNext()){
                                for(String id:indexIDProvider.readNext()){
                                    Row row = new Row();
                                    for (String column : result.getQuery().selectedColumns) {
                                        if (column.equals(XML.getTable(parsedquery.tableName,Worker.currentlyWorking).getKey().getName())) {
                                            row.getValues().add(String.valueOf(id));
                                        } else {
                                            row.getValues().add(simpleconnection.getColumn(id + "", column));
                                        }
                                    }
                                    allPage.getRows().add(row);
                                }
                            }
                        }
                    }
                    pkconnection.closeConnection();
                    pkidconnection.closeConnection();
                    simpleconnection.closeConnection();
                    initResultColumnMap();
                } else {
                    simpleSelectQuery.select(parsedquery, result);
                    idIterator = result.getIDs().iterator();
                    int id;
                    resultColumns = result.getQuery().selectedColumns;
                    RedisConnector connection = new RedisConnector();
                    connection.connect();
                    connection.select(result.getSelectedTable().getSlotNumber());
                    while (idIterator.hasNext()) {
                        id = idIterator.next();
                        Row row = new Row();
                        for (String column : result.query.selectedColumns) {
                            if (column.equals(result.getSelectedTable().getKey().getName())) {
                                row.getValues().add(String.valueOf(id));
                            } else {
                                row.getValues().add(connection.getColumn(id + "", column));
                            }
                        }
                        allPage.getRows().add(row);
                    }
                    connection.closeConnection();
                    sortByGroupingColumn();
                }
            }
        } catch (comm.ServerException ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    private void initResultColumnMap() {
        for (String column : selectedColumns) {
            for (int i = 0; i < resultColumns.size(); ++i) {
                if (resultColumns.get(i).equals(removeOperator(column))) {
                    resultColumnMap.put(column, i);
                    break;
                }
            }
        }
    }

    private int getColumnIndex(String column) throws ServerException {
        try {
            return resultColumnMap.get(column);
        } catch (NullPointerException ex) {
            System.out.println(column);
        }
        return -1;
    }

    private boolean columnIsGroupByColumn(String column) {
        for (String op : operationsList) {
            if (column.contains(op)) return false;
        }
        return true;
    }

    private void extractColumnsToGroupBy(String queryString) throws ServerException {
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
        String column;
        while (columnTokenizer.hasMoreTokens()) {
            column = columnTokenizer.nextToken();
            selectedColumns.add(column);
            if (columnIsGroupByColumn(column)) {
                groupingColumn = column;
            }
        }
        String[] splitAtGROUP = splitAtFrom[1].split("GROUP BY");
        if (splitAtGROUP.length == 1) {
            splitAtGROUP = splitAtFrom[1].split("group by");
        }

        selectAllQuery = "select * from " + splitAtGROUP[0];
        if (splitAtGROUP[1].contains("HAVING") || splitAtGROUP[1].contains("having")) {
            String[] splitAtHAVING = splitAtGROUP[1].split("HAVING");
            if (splitAtHAVING.length == 1) {
                splitAtHAVING = splitAtGROUP[1].split("having");
            }

            String[] havingConstraints = splitAtHAVING[1].trim().split("\\s*AND|and+\\s*");//We have yet to support or
            parseHavingConditions(havingConstraints);
        }

        selectAllQuery = selectAllQuery.replaceFirst("\\*", addCommaBetweenColumnsAndRemoveOperators(selectedColumns));

    }

    private String removeOperator(String column) {
        for (String op : operationsList) {
            column = column.replaceFirst(op, "");
        }
        return column.replaceAll("\\(", "").replaceAll("\\)", "");
    }

    private String addCommaBetweenColumnsAndRemoveOperators(ArrayList<String> columns) {
        String ret = "";
        for (int i = 0; i < columns.size() - 1; ++i) {
            ret += removeOperator(columns.get(i)) + ",";
        }
        return ret + removeOperator(columns.get(columns.size() - 1));
    }

    private void parseHavingConditions(String[] havingConstraints) throws ServerException {
        for (int i = 0; i < havingConstraints.length; i++) {
            String constraint = havingConstraints[i];
            String equalityPattern = " *([A-Za-z0-9().]+) *= *(.+) *";
            String biggerPattern = " *([A-Za-z0-9().]+) *> *(.+) *";
            String smallerPattern = " *([A-Za-z0-9().]+) *< *(.+) *";

            Pattern pattern = Pattern.compile(equalityPattern);
            Matcher matcher = pattern.matcher(constraint);
            String column = null;
            String value = null;

            if (matcher.find()) {
                column = matcher.group(1).trim();
                value = matcher.group(2).trim();
                havingConditions.add(new accumRelationship(column, "=", Double.parseDouble(value)));
            } else {
                pattern = Pattern.compile(biggerPattern);
                matcher = pattern.matcher(constraint);
                if (matcher.find()) {
                    column = matcher.group(1).trim();
                    value = matcher.group(2).trim();
                    havingConditions.add(new accumRelationship(column, ">", Double.parseDouble(value)));
                } else {
                    pattern = Pattern.compile(smallerPattern);
                    matcher = pattern.matcher(constraint);
                    if (matcher.find()) {
                        column = matcher.group(1).trim();
                        value = matcher.group(2).trim();
                        havingConditions.add(new accumRelationship(column, "<", Double.parseDouble(value)));
                    } else {
                        throw new comm.ServerException("Error building query: invalid opertaor in " + constraint);
                    }
                }
            }
        }
    }
}
