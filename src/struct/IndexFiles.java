package struct;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexFiles {
    public IndexFiles() {
        this.indexFiles = new ArrayList<>();
    }

    @JacksonXmlProperty(localName = "IndexFile")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IndexFile> indexFiles;

    public List<IndexFile> getIndexFiles() {
        if (indexFiles == null) {
            this.indexFiles = new ArrayList<>();
        }
        return indexFiles;
    }

    public void setIndexFiles(List<IndexFile> indexFiles) {
        this.indexFiles = indexFiles;
    }

    @Override
    public String toString() {
        return indexFiles+"";
    }
}
