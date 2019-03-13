package struct;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForeignKeys {

    public ForeignKeys() {
        this.foreignKeyList = new ArrayList<>();
    }

    @JacksonXmlProperty(localName = "ForeignKey")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ForeignKey> foreignKeyList;

    public List<ForeignKey> getForeignKeyList() {

        if (foreignKeyList == null)
            return Collections.emptyList();
        return foreignKeyList;
    }

    public void setForeignKeyList(List<ForeignKey> foreignKeyList) {
        this.foreignKeyList = foreignKeyList;
    }

    @Override
    public String toString() {
        return foreignKeyList+"";
    }
}
