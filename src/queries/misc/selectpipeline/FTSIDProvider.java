package queries.misc.selectpipeline;

import comm.Worker;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

public class FTSIDProvider implements IDSource{
    private final int resultPerQuery = 1000;
    private final ScanParams params;
    private int tableSlot;
    private String cursor;
    private boolean readAll;

    public FTSIDProvider(int slot) {
        this.tableSlot = slot;
        cursor = "0";
        readAll = false;
        params = new ScanParams();
        params.count(resultPerQuery);
    }

    @Override
    public List<String> readNext() {
        Worker.RDB.select(tableSlot);
        ScanResult<String> result= Worker.RDB.scan(cursor,params);
        if(result.getCursor().equals("0")){
            readAll = true;
        }
        cursor = result.getCursor();
        return result.getResult();
    }

    @Override
    public boolean hasNext() {
        return !readAll;
    }
}
