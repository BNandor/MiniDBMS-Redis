package struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Databases {


    public List<Database> getDatabaseList() {
        if (databaseList == null)
            return Collections.emptyList();
        return databaseList;
    }

    public void setDatabaseList(List<Database> databaseList) {
        this.databaseList = databaseList;
    }

    @JsonProperty("Database")
    @JacksonXmlProperty(localName = "Database")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Database> databaseList;

    public Databases() {
        this.databaseList = new ArrayList<>();
    }

    public Databases(List<Database> dbs) {
        this.databaseList = dbs;
    }

    @Override
    public String toString() {
        return "Databases :\n" + databaseList;
    }

    public TreeItem<String> visit() {
        TreeItem<String> root = new TreeItem<>("Databases");

        for (Database database : databaseList) {
            root.getChildren().add(database.visit());
        }

        return root;
    }
}
