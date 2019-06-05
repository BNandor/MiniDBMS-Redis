package queries.misc.selectpipeline;

import persistence.RedisConnector;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

public class IndexIDProvider implements IDSource {
    private int indexSlot;
    private String indexedColumnValue;
    private String cursor;
    private boolean readAll;
    private RedisConnector redisConnector;
    private String match;
    private ScanParams params;

    public IndexIDProvider(int indexSlot, String indexedColumnValue, RedisConnector redisConnector) {
        this.indexSlot = indexSlot;
        this.indexedColumnValue = indexedColumnValue;
        cursor = "0";
        readAll = false;
        this.redisConnector = redisConnector;
    }

    public IndexIDProvider(int indexSlot, String indexedColumnValue, String match, RedisConnector redisConnector) {
        this.indexSlot = indexSlot;
        this.indexedColumnValue = indexedColumnValue;
        cursor = "0";
        readAll = false;
        this.redisConnector = redisConnector;
        this.match = match;
        params = new ScanParams();
        params.match(match);
        params.count(1000);
    }

    @Override
    public List<String> readNext() {
        redisConnector.select(indexSlot);
        ScanResult<String> result;

        if (match != null) {
            result = redisConnector.setscanmatch(indexedColumnValue, cursor, params);
        } else {
            result = redisConnector.setscan(indexedColumnValue, cursor);
        }
        if (result.getCursor().equals("0")) {
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
