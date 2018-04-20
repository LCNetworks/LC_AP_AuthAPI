package gehc.gst.smartx.oauth2.sso.service;

public interface CacheService {
    public String get(String key);
    public boolean put(String key,String value,Integer expireTime);
    public void remove(String key);
    public boolean exists(String key);

}
