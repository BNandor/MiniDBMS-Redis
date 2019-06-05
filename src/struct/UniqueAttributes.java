package struct;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UniqueAttributes {
    public UniqueAttributes() {
        this.uniqueList = new ArrayList<>();
    }

    @JsonProperty("uniqueAttribute")
    @JacksonXmlProperty(localName = "uniqueAttribute")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Unique> uniqueList;

    public List<Unique> getUniqueList() {
        if (uniqueList == null)
            return Collections.emptyList();
        return uniqueList;
    }

    public void setUniqueList(List<Unique> uniqueList) {
        this.uniqueList = uniqueList;
    }

    @Override
    public String toString() {
        return uniqueList + "";
    }
}
