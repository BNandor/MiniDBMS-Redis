package queries.misc.selectpipeline;

import persistence.RedisConnector;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

public class FTSIDProvider implements IDSource {
    private final int resultPerQuery = 1000;
    private final ScanParams params;
    private int tableSlot;
    private String cursor;
    private boolean readAll;
    private RedisConnector redisConnector;

    public FTSIDProvider(int slot, RedisConnector connector, boolean onlyNumbers) {
        this.tableSlot = slot;
        cursor = "0";
        readAll = false;
        params = new ScanParams();
        params.count(resultPerQuery);
        this.redisConnector = connector;
        if (onlyNumbers)
            params.match("[0-9]*");
    }

    @Override
    public List<String> readNext() {
        redisConnector.select(tableSlot);
        ScanResult<String> result = redisConnector.scan(cursor, params);
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
