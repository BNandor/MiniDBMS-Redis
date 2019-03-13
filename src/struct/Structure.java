package struct;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Structure {

    public Structure() {
        this.attributeList = new ArrayList<>();
    }

    @JacksonXmlProperty(localName = "attribute")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Attribute> attributeList;

    public List<Attribute> getAttributeList() {
        if (attributeList == null)
            return Collections.emptyList();
        return attributeList;
    }

    public void setAttributeList(List<Attribute> attributeList) {
        this.attributeList = attributeList;
    }

    @Override
    public String toString() {
        return attributeList+"";
    }
}
