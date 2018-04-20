package gehc.gst.smartx.oauth2.sso.controller;

import cn.com.inhand.tools.net.GenericURL;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;
import static springfox.documentation.builders.RequestHandlerSelectors.any;

/**
 * Created by hanchuanjun on 16/7/25.
 */
@Configuration
@EnableSwagger2
@ComponentScan("gehc.gst.smartx.oauth2.controller")
public class SwaggerConfig {
    @Value("${inhand.product.url}") private String url;
    @Value("${inhand.product.path}") private String path;
    @Bean
    public Docket swaggerSpringMvcPlugin() {

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("core")
                .select()
                .apis(any())
                .paths(paths())
                .build()
                .apiInfo(apiInfo());
        if (url != null && !url.equals("")){
            GenericURL addr = GenericURL.createGenericURL(url);
            if (addr.getProtocol().equalsIgnoreCase("http")) {
                if (addr.getPort() == 80) {
                    docket.host(addr.getHost());
                } else {
                    docket.host(addr.getHost() + ":" + addr.getPort());
                }
            }else if(addr.getProtocol().equalsIgnoreCase("https")){
                if (addr.getPort() == 443) {
                    docket.host(addr.getHost());
                } else {
                    docket.host(addr.getHost() + ":" + addr.getPort());
                }
                Set<String> protocols = new HashSet<String>();
                protocols.add("https");
                docket.protocols(protocols);
            }
        }
        if (path != null && !path.equals("")){
            docket.pathMapping(path);
        }
        return docket;
    }


    //Here is an example where we select any api that matches one of these paths
    private Predicate<String> paths() {
        return or(
                //regex("/business.*"),
                // regex("/some.*"),
                // regex("/contacts.*"),
                // regex("/pet.*"),
                regex("/api.*"),
                regex("/oauth2.*"));
    }

    private ApiInfo apiInfo() {//public ApiInfo(String title, String description, String version, String termsOfServiceUrl, Contact contact, String license, String licenseUrl)
        Contact contact = new Contact("Hanchuanjun", "", "han@inhand.com.cn");
        ApiInfo apiInfo = new ApiInfo(
                "GEHC SmartX SSO-OAuth模块api",
                "GEHC SmartX SSO-OAuth模块服务api",
                "v0.0.1",
                "初始化版本",
                contact,
                "",
                "");
        return apiInfo;
    }
}
