package struct;

public class ForeignKey {

    public ForeignKey() {
    }

    public ForeignKey(String name, String refTableName, String refTableAttributeName) {
        this.name = name;
        this.refTableName = refTableName;
        this.refTableAttributeName = refTableAttributeName;
    }

    private String name;
    private String refTableName;
    private String refTableAttributeName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRefTableName() {
        return refTableName;
    }

    public void setRefTableName(String refTableName) {
        this.refTableName = refTableName;
    }

    public String getRefTableAttributeName() {
        return refTableAttributeName;
    }

    public void setRefTableAttributeName(String refTableAttributeName) {
        this.refTableAttributeName = refTableAttributeName;
    }

    @Override
    public String toString() {
        return name + " that references " + refTableAttributeName + " in  table " + refTableName;
    }
}
