package queries.misc.selectresultprotocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class Row {
    @JsonProperty("val")
    private ArrayList<String> values;

    public Row() {
        values = new ArrayList<>();
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }
}
