package queries.misc.selectpipeline;

import comm.Worker;
import persistence.RedisConnector;
import redis.clients.jedis.ScanResult;

import java.util.List;

public class IndexIDProvider implements IDSource
{
    private int indexSlot;
    private String indexedColumnValue;
    private String cursor;
    private boolean readAll;
    private RedisConnector redisConnector;
    public IndexIDProvider(int indexSlot, String indexedColumnValue,RedisConnector redisConnector) {
        this.indexSlot = indexSlot;
        this.indexedColumnValue = indexedColumnValue;
        cursor="0";
        readAll=false;
        this.redisConnector =redisConnector;
    }

    @Override
    public List<String> readNext() {
        redisConnector.select(indexSlot);
        ScanResult<String> result= redisConnector.setscan(indexedColumnValue,cursor);
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
