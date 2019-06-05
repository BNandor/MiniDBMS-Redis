package struct;

public class Attribute {
    private String name;
    private String type;
    private int length;
    private int isnull;

    public Attribute() {
    }

    public Attribute(String name, String type, int length, int isnull) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.isnull = isnull;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getIsnull() {
        return isnull;
    }

    public void setIsnull(int isnull) {
        this.isnull = isnull;
    }

    @Override
    public String toString() {
        return name + ":" + type + ":" + length + ":" + isnull;
    }
}
