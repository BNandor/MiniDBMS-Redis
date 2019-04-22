package queries.misc.selectpipeline;

import java.util.List;

public interface IDSource {
    List<String> readNext();
    boolean hasNext();
}
