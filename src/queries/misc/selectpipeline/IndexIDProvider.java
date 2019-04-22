package queries.misc.selectpipeline;

import comm.Worker;
import redis.clients.jedis.ScanResult;

import java.util.List;

public class IndexIDProvider implements IDSource
{
    private int indexSlot;
    private String indexedColumnValue;
    private String cursor;
    private boolean readAll;
    public IndexIDProvider(int indexSlot, String indexedColumnValue) {
        this.indexSlot = indexSlot;
        this.indexedColumnValue = indexedColumnValue;
        cursor="0";
        readAll=false;
    }

    @Override
    public List<String> readNext() {
        Worker.RDB.select(indexSlot);
        ScanResult<String> result= Worker.RDB.setscan(indexedColumnValue,cursor);
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
