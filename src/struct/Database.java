package struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.control.TreeItem;

public class Database {
    public Database() {
        //this.databaseName = databaseName;
        this.tables = new Tables();
    }

    @JsonProperty("databaseName")
    private String databaseName;
    private Tables tables;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Tables getTables() {

        if (tables == null) {
            tables = new Tables();
        }
        return tables;
    }

    public void setTables(Tables tables) {
        this.tables = tables;
    }

    @Override
    public String toString() {
        return "Database: " + databaseName + " has \n" + tables + "\n";
    }

    public TreeItem<String> visit() {
        TreeItem<String> root = new TreeItem<>(databaseName);
        if (tables == null) {
            root.getChildren().add(new TreeItem<>("Tables"));
        } else {
            root.getChildren().add(tables.visit());
        }
        return root;
    }
}
