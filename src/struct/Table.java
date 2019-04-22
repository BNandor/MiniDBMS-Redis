package struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;

public class Table {
    public Table(){

    }
    public Table(String tableName, int slotNumber, int rowLength, Structure tableStructure, PrimaryKey key, ForeignKeys foreignKeys, UniqueAttributes uniqueAttributes, IndexFiles indexFiles) {
        this.tableName = tableName;
        this.slotNumber = slotNumber;
        this.rowLength = rowLength;
        this.tableStructure = tableStructure;
        this.key = key;
        this.foreignKeys = foreignKeys;
        this.uniqueAttributes = uniqueAttributes;
        this.indexFiles = indexFiles;
    }

    @JsonProperty("TableName")
    private String tableName;
    @JsonProperty("slotNumber")
    private int slotNumber;
    @JsonProperty("rowLength")
    private int rowLength;

    @JsonProperty("tableStructure")
    private Structure tableStructure;
    private PrimaryKey key;
    @JsonProperty("foreignKeys")
    private ForeignKeys foreignKeys;


    private UniqueAttributes uniqueAttributes;
    @JsonProperty("indexFiles")
    private IndexFiles indexFiles;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public int getRowLength() {
        return rowLength;
    }

    public void setRowLength(int rowLength) {
        this.rowLength = rowLength;
    }

    public Structure getTableStructure() {
        return tableStructure;
    }

    public void setTableStructure(Structure tableStructure) {
        this.tableStructure = tableStructure;
    }

    public PrimaryKey getKey() {
        return key;
    }

    public void setKey(PrimaryKey key) {
        this.key = key;
    }

    public ForeignKeys getForeignKeys() {
        if(foreignKeys == null)
            foreignKeys = new ForeignKeys();
        return foreignKeys;
    }

    public void setForeignKeys(ForeignKeys foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public UniqueAttributes getUniqeAttributes() {
        if(uniqueAttributes == null)
            uniqueAttributes = new UniqueAttributes();
        return uniqueAttributes;
    }

    public void setUniqeAttributes(UniqueAttributes uniqeAttributes) {
        this.uniqueAttributes = uniqeAttributes;
    }

    public IndexFiles getIndexFiles() {
        if (indexFiles == null)
            indexFiles = new IndexFiles();
            return indexFiles;
    }

    public void setIndexFiles(IndexFiles indexFiles) {
        this.indexFiles = indexFiles;
    }

    @Override
    public String toString() {
        return "\ntable "+tableName+" that is stored in  slot no: "+slotNumber + "\n that has "+ rowLength+" rows\n"+
                "that has the following structure \n"+tableStructure+"\n primary key "+key+"\nforeign keys "+foreignKeys+
                "\nunique attributes "+uniqueAttributes+"\nindex files "+indexFiles;

    }

    public TreeItem<String> visit() {
        TreeItem<String> root = new TreeItem<>(tableName);
        return root;
    }
}
