package struct;

public class IndexFile {
    private int indexFileName;

    public IndexFile() {
    }

    public IndexFile(int indexFileName, String name) {
        this.indexFileName = indexFileName;
        this.name = name;
    }

    public int getIndexFileName() {
        return indexFileName;
    }

    public void setIndexFileName(int indexFileName) {
        this.indexFileName = indexFileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    @Override
    public String toString() {
        return name;
    }
}
