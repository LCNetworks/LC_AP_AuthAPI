package gehc.gst.smartx.oauth2.sso.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import gehc.gst.smartx.db.core.bean.IhUsers;
import gehc.gst.smartx.db.core.dto.UserDto;
import gehc.gst.smartx.db.core.service.CoreService;
import gehc.gst.smartx.oauth2.sso.service.CacheService;
import io.swagger.annotations.ApiOperation;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/oauth2")
public class SSOAuthController {
    public static final String PREFIX_ACCESS_TOKEN = "ATOKEN:";
    public static final String PREFIX_REFRESH_TOKEN = "RTOKEN:";
    public static final String PREFIX_USERID = "UID:";
    //@Autowired
    CoreService service;
    //@Autowired
    JdbcTemplate jdbcTemplate;
    @Value("${spring.datasource.url}")
    String dbUrl;

    //private RedisTemplate redisTemplate;
    @Autowired
    private CacheService cache;

    @Value("${config.sso.clientId}")
    private  String clientId;
    @Value("${config.sso.clientSecret}")
    private  String clientSecrete;
    @Value("${config.sso.url}")
    private  String ssoUrl;
    @Value("${config.sso.scope}")
    private  String scope;
    @Value("${gehc.smartx.roles.standard.prefixes}")
    private  String standard_roles_prefixes;
    @Value("${gehc.smartx.roles.limited.prefixes}")
    private  String limited_roles_prefixes;
    @Value("${gehc.smartx.roles.standard.ids}")
    private  String standard_roles_ids;
    @Value("${gehc.smartx.roles.limited.ids}")
    private  String limited_roles_ids;



    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        String dbType = dbUrl.indexOf("mysql")>0?"mysql":"mssql";
        service = new CoreService(this.jdbcTemplate,dbType);
    }


    @RequestMapping(value = "/permissions", method=RequestMethod.GET)
    public @ResponseBody Map<String,Object> getUserPermissions(
            @RequestParam(name="access_token",required = true) String access_token,
            @RequestParam(name="module",required = false) String module,
            @RequestParam(name="resource",required = false) String resource){
        Map<String,Object> map = new HashMap<String,Object>();
        try{
            String uid = (String)this.cache.get(PREFIX_ACCESS_TOKEN+access_token);
            String json = this.cache.get(PREFIX_USERID+uid);
            ObjectMapper mapper = new ObjectMapper();
            UserDto user = (uid == null ?null:(json!=null?mapper.readValue(json,UserDto.class):null));
            if (user == null){
                map.put("result","error");
                map.put("code",401);
                map.put("description","Invalid access_token or access_token is timeout!");
            }else{
                try {
                    Integer ts =user.getUser().getTs();

                    Long now=System.currentTimeMillis();
                    if (user == null) {
                        map.put("result", "error");
                        map.put("code", 404);
                        map.put("description", "access_token is timeout or not exist");
                    } else {
                        map.put("result", "ok");
                        map.put("code", 200);
                        map.put("description", "ok");
                        map.put("permissions", user.validateModulePermissions(module,resource));
                    }
                }catch(Exception ex){

                    map.put("result","error");
                    map.put("code",401);
                    map.put("description","Invalid access_token!");
                }
            }
        }catch (Exception e){
            e.printStackTrace();

            map.put("result","error");
            map.put("code",500);
            map.put("description",e.getLocalizedMessage());
        }
        return map;
    }


    @RequestMapping(value = "/access_token/{access_token}", method=RequestMethod.GET)
    public @ResponseBody Map<String,Object> getUser0(@PathVariable String access_token,
                                                     @RequestParam(name="module",required = false) String module,
                                                     @RequestParam(name="action",required = false) String action) {
        return this.getUser(access_token,module,action);
    }

    @RequestMapping(value = "/accessToken/{access_token}", method=RequestMethod.GET)
    public @ResponseBody Map<String,Object> getUser(@PathVariable String access_token,
                                                    @RequestParam(name="module",required = false) String module,
                                                    @RequestParam(name="resource",required = false) String resource){
        Map<String,Object> map = new HashMap<String,Object>();
        try{
            String uid = (String)this.cache.get(PREFIX_ACCESS_TOKEN+access_token);
            String json = this.cache.get(PREFIX_USERID+uid);
            ObjectMapper mapper = new ObjectMapper();
            UserDto user = (uid == null ?null:(json!=null?mapper.readValue(json,UserDto.class):null));
            if (user == null){
                map.put("result","error");
                map.put("code",401);
                map.put("description","Invalid access_token or access_token is timeout!");
            }else{
                try {
                    Integer ts =user.getUser().getTs();
                    Long now=System.currentTimeMillis();
                    if (user == null) {
                        map.put("result", "error");
                        map.put("code", 404);
                        map.put("description", "access_token is timeout or not exist");
                    } else {
                        user.setPermissions(user.validateModulePermissions(module,resource));
                        map.put("result", "ok");
                        map.put("code", 200);
                        map.put("description", "ok");
                        map.put("user", user);
                    }
                }catch(Exception ex){

                    map.put("result","error");
                    map.put("code",401);
                    map.put("description","Invalid access_token!");
                }
            }
        }catch (Exception e){
            e.printStackTrace();

            map.put("result","error");
            map.put("code",500);
            map.put("description",e.getLocalizedMessage());
        }
        return map;
    }

    @ApiOperation(value = "让OAuth模块更新用户信息到缓存", httpMethod = "POST", response = Map.class, notes = "refresh",consumes = "application/x-www-form-urlencoded")
    @RequestMapping(value = "/users/{userId}", method = RequestMethod.POST)//,consumes = "application/x-www-form-urlencoded")
    public
    @ResponseBody
    Map<String,Object> refreshUserInfo(@PathVariable String userId,
                                       @RequestParam Map<String,Integer> params)  {
        Map<String,Object> map = new HashMap<>();
        try {
            UserDto user = this.service.getUserDto(userId, null);//module);
            if (user != null) {
                ObjectMapper mapper = new ObjectMapper();
                this.cache.put(PREFIX_USERID + userId, mapper.writeValueAsString(user),null);

                map.put("result", "ok");
                map.put("code", 200);
                map.put("description", "ok");
            }else{
                map.put("result", "error");
                map.put("code", 404);
                map.put("description", "No such user");
            }
        }catch (Exception e){

            map.put("result", "error");
            map.put("code", 500);
            map.put("description", e.getMessage());
        }
        return map;
    }


    @ApiOperation(value = "登录", httpMethod = "POST", response = Map.class, notes = "login",consumes = "application/x-www-form-urlencoded")
    @RequestMapping(value = "/access_token", method = RequestMethod.POST)//,consumes = "application/x-www-form-urlencoded")
    public
    @ResponseBody
    Object login0(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
        return this.login(request);
    }

    @ApiOperation(value = "登录", httpMethod = "POST", response = Map.class, notes = "login",consumes = "application/x-www-form-urlencoded")
    @RequestMapping(value = "/accessToken", method = RequestMethod.POST)//,consumes = "application/x-www-form-urlencoded")
    public
    @ResponseBody
    Object login(HttpServletRequest request) throws URISyntaxException, OAuthSystemException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            //构建OAuth请求
            OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
            UserDto user = null;
            Map<String,String> retSSO =null;

            String client_id = oauthRequest.getClientId();
            String client_secret = oauthRequest.getClientSecret();
            //检查提交的客户端id是否正确
            if (!this.clientId.equals(client_id)) {
                OAuthResponse response = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription("Invalid client id")
                        .buildJSONMessage();
                return new ResponseEntity(
                        response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }

            // 检查客户端安全KEY是否正确
            if (!this.clientSecrete.equals(client_secret)) {
                OAuthResponse response = OAuthASResponse
                        .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                        .setErrorDescription("Invalied client secret")
                        .buildJSONMessage();
                return new ResponseEntity(
                        response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }


            //String authCode = oauthRequest.getParam(OAuth.OAUTH_CODE);
            // 检查验证类型，此处只检查AUTHORIZATION_CODE类型，其他的还有PASSWORD或REFRESH_TOKEN
            if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
                    GrantType.AUTHORIZATION_CODE.toString())) {

                if (true) {
                    OAuthResponse response = OAuthASResponse
                            .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription("Unsupported authization mode.") //暂不支持
                            .buildJSONMessage();
                    return new ResponseEntity(
                            response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
                }
            } else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
                    GrantType.PASSWORD.toString())) {
                String uId = oauthRequest.getUsername();
                String password = oauthRequest.getPassword();
                Set<String> scopes = oauthRequest.getScopes();
                String module = scopes.isEmpty()?null:scopes.iterator().next();

                // boolean rememberMe = (user.getRememberFlag() != null && user.getRememberFlag() == 1);
                //String host = request.getRemoteHost();
                /*HostAuthenticationToken authToken = new UsernamePasswordToken(account, password, rememberMe, host);
                try {
                    SecurityUtils.getSubject().login(authToken);

                    Subject currentUser = SecurityUtils.getSubject();
                    user = (LoginUser) currentUser.getSession().getAttribute("currentUser");
                } catch (Exception e) {

                }*/

                user = this.service.getUserDto(uId,null);//module);
                retSSO = this.ssoLogin(uId,password);

                if (user != null){
                    IhUsers u = user.getUser();
                    if(u.getState()!=null&&u.getState().equals("locked")){
                        OAuthResponse response = OAuthASResponse
                                .errorResponse(423)
                                .setError("user_locked")
                                .setErrorDescription("The user is locked") //暂不支持
                                .buildJSONMessage();
                        return new ResponseEntity(
                                response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
                    }
                    u.setTs((int)(System.currentTimeMillis()/1000));
                    u.setOnline(1);
                    this.service.updateUser(u);

                }else{
                    String ids = this.getRoleIds4NonUser(uId);
                    if (ids != null && !ids.equals("")){
                        user = this.service.getNonUserDto(uId,ids);
                        IhUsers u= user.getUser();
                        u.setTs((int)(System.currentTimeMillis()/1000));
                        u.setOnline(1);
                        u.setState("active");
                        u.setIhRolesIds(ids);
                        this.service.newUser(u);
                    }
                }

            } else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(
                    GrantType.REFRESH_TOKEN.toString())) {
                String refreshToken = oauthRequest.getRefreshToken();
                String json = this.cache.get(PREFIX_REFRESH_TOKEN+refreshToken);
                retSSO = null;
                if (json != null){
                    retSSO = mapper.readValue(json,Map.class);
                    Map<String,Object> map = this.getUser(retSSO.get("access_token"),null,null);
                    user = (UserDto)map.get("user");
                    map.clear();
                    this.cache.remove(PREFIX_ACCESS_TOKEN+retSSO.get("access_token"));
                    this.cache.remove(PREFIX_REFRESH_TOKEN+refreshToken);
                }

                retSSO = this.ssoRefreshToken(refreshToken);
            }



                //生成Access Token
                String accessToken = retSSO.get("access_token");
                String  refreshToken = retSSO.get("refresh_token");

                String name = user==null?null:user.getUser().getName();

                this.cache.put(PREFIX_REFRESH_TOKEN+refreshToken, mapper.writeValueAsString(retSSO),86400);


                if (user != null && user.getUser() != null) {
                    this.cache.put(PREFIX_USERID + user.getUser().getIhUsersId(), mapper.writeValueAsString(user), null);
                }

                this.cache.put(PREFIX_ACCESS_TOKEN + accessToken, user.getUser().getIhUsersId(), 3600);


                //生成OAuth响应
                OAuthResponse response = OAuthASResponse
                        .tokenResponse(HttpServletResponse.SC_OK)
                        .setAccessToken(accessToken)
                        .setParam("userName", name)
                        .setRefreshToken(refreshToken)
                        .setExpiresIn("3600")
                        .buildJSONMessage();

                //根据OAuthResponse生成ResponseEntity
                return new ResponseEntity(
                        response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));

        } catch (OAuthProblemException e) {
            e.printStackTrace();
            //构建错误响应
            OAuthResponse res = OAuthASResponse
                    .errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
                    .buildJSONMessage();
            return new ResponseEntity(res.getBody(), HttpStatus.valueOf(res.getResponseStatus()));
        } catch (Exception ex) {
            ex.printStackTrace();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("result", "error");
            map.put("description", ex.getMessage());
            return new ResponseEntity(map, HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String,String> ssoLogin(String account,String password)throws OAuthProblemException,IOException{
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        //MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        //JSONObject jsonObj = JSONObject.fromObject(params);
        // 封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>();
        params.add("grant_type","password");
        params.add("client_id",this.clientId);
        params.add("client_secret",this.clientSecrete);
        params.add("password",password);
        params.add("username",account);
        params.add("scope",scope);

        HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        String url = this.ssoUrl;/* + "?grant_type=password&client_id="
                +this.clientId
                +"&client_secret="
                +this.clientSecrete
                +"&password="
                +password
                +"&username="
                +account
                +"&scope="
                +this.scope;*/
        String result = rest.postForObject(url, formEntity, String.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> map=mapper.readValue(result,Map.class);
        if (map.containsKey("access_token"))
            return map;
        else{
            throw  OAuthProblemException.error("Invlid user","Invalid user");
        }
    }

    private Map<String,String> ssoRefreshToken(String refreshToken)throws OAuthProblemException,IOException{
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        //MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        //JSONObject jsonObj = JSONObject.fromObject(params);
        // 封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>();
        params.add("grant_type","refresh_token");
        params.add("client_id",this.clientId);
        params.add("client_secret",this.clientSecrete);
        params.add("refresh_token",refreshToken);
        params.add("scope",scope);

        HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        String url = this.ssoUrl;/* + "?grant_type=password&client_id="
                +this.clientId
                +"&client_secret="
                +this.clientSecrete
                +"&password="
                +password
                +"&username="
                +account
                +"&scope="
                +this.scope;*/
        String result = rest.postForObject(url, formEntity, String.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> map=mapper.readValue(result,Map.class);
        if (map.containsKey("access_token"))
            return map;
        else{
            throw  OAuthProblemException.error("Invlid refresh token","Invalid refresh token");
        }
    }

    private String getRoleIds4NonUser(String uId){
        for (String pr:this.limited_roles_prefixes.split(",")){
            if (uId.startsWith(pr)){
                return this.limited_roles_ids;
            }
        }

        for (String pr:this.standard_roles_prefixes.split(",")){
            if (uId.startsWith(pr)){
                return this.standard_roles_ids;
            }
        }
        return null;


    }

}
