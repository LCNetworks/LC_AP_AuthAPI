package gehc.gst.smartx.oauth2.sso.controller;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hanchuanjun on 16/12/1.
 */
@RestController
@RequestMapping("/oauth2")
public class ModuleInfoController {

    @Value("${product.build.version}")
    private String version;
    @Value("${product.build.time}")
    private String build_time;
    @Value("${product.build.branch}")
    private String branch;
    @Value("${product.build.comment}")
    private String comment;



    @Value("${spring.cloud.zookeeper.connect-string}")
    private String connect_string;

    /**
     * 本地服务实例的信息
     * @return
    @GetMapping("/instance-info")
    public ServiceInstance showInfo() {
        ServiceInstance localServiceInstance = org.springframework.cloud.client.serviceregistry.Registration;
        return localServiceInstance;
    }

     */



    @GetMapping("/version")
    public Map<String,Object> showVersion() {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("version",this.version);
        map.put("build_time",this.build_time);
        map.put("branch",this.branch);
        map.put("comment",this.comment);
        return map;
    }

    @RequestMapping(value="/zk",method = RequestMethod.GET)
    public String zkGet(@RequestParam(name="path") String path){
        Watcher watcher = new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("------------------receive event:"+ event);
            }
        };
        String value = null;
        try{
            final ZooKeeper zk = new ZooKeeper(connect_string,60000,watcher);
            final byte[] data = zk.getData(path,watcher,null);
            value = new String(data);
            zk.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        return path+"="+value;
    }

}

