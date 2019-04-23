package queries.misc.selectresultprotocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Page implements Serializable {
    private int pageNumber;
    @JsonProperty("Row")
    @JacksonXmlProperty(localName = "Row")
    @JacksonXmlElementWrapper(useWrapping = false)
    private ArrayList<Row> rows;

    public Page() {
        rows = new ArrayList<>();
    }

    public void setRows(ArrayList<Row> rows) {
        this.rows = rows;
    }

    public ArrayList<Row> getRows() {
        return rows;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }


}
