package struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tables {

    public Tables() {
        this.tableList = new ArrayList<>();
    }

    public List<Table> getTableList() {
        if (tableList == null){
            return Collections.emptyList();
        }
        return tableList;
    }

    public void setTableList(List<Table> tableList) {
        this.tableList = tableList;
    }

    @JsonProperty("Table")
    @JacksonXmlProperty(localName = "Table")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Table> tableList;

    @Override
    public String toString() {
        return tableList+"";
    }
    public TreeItem<String> visit() {
        TreeItem<String> root = new TreeItem<>("Tables");

        if (tableList == null) {
            return root;
        }

        for (Table table : tableList) {
            root.getChildren().add(table.visit());
        }

        return root;
    }
}
