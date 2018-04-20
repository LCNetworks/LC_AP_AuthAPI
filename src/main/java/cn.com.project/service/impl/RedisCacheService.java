package gehc.gst.smartx.oauth2.sso.service.impl;

import com.mysql.jdbc.TimeUtil;
import gehc.gst.smartx.oauth2.sso.service.CacheService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jredis.JredisPool;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Data
@Service
public class RedisCacheService implements CacheService {
    /*@Autowired
    private RedisTemplate redisTemplate;

    public RedisCacheService(RedisTemplate template){
        this.redisTemplate = template;
    }*/
    @Autowired
    JedisPool pool;
    @Override
    public String get(String key) {
        String result = null;
        Jedis jedis = pool.getResource();
        try {

            result = jedis.get(key);
        }catch (Exception e){
            throw new JedisException(e.getMessage());
        }finally {

            try{
                jedis.close();;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public boolean put(String key, String value, Integer expireTime) {
        boolean result = false;

        Jedis jedis= pool.getResource();
        try {
            if (expireTime != null  && expireTime > 0) {
                jedis.setex(key,  expireTime,value);
            }else{
                jedis.set(key,value);
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try{
                jedis.close();;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public void remove(String key) {
        Jedis jedis= pool.getResource();
        try {
            jedis.del(key);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try{
                jedis.close();;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean exists(String key) {
        Jedis jedis = pool.getResource();
        boolean result=false;
        try {
            result = jedis.exists(key);
        }catch (Exception e){
            throw new JedisException(e.getMessage());
        }finally {

            try{
                jedis.close();;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }
}
