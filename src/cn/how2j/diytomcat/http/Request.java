package cn.how2j.diytomcat.http;

import cn.how2j.diytomcat.catalina.Connector;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Engine;
import cn.how2j.diytomcat.catalina.Service;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/*
knowledge point:
IoUtil.readLines(source(stream),target(array));
StrUtil.remove(source,prefix);

Question: uri ?
 */

public class Request extends BaseRequest{

    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private String method;
    private String queryString;
    private Map<String,String[]> parameterMap;
    private Map<String,String> headerMap;
    private Cookie[] cookies;
    private HttpSession session;
    private Connector connector;
    private boolean forward;
    private Map<String, Object> attributesMap;

    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethod();
        parseParameters();
        parseHeaders();
        parseCookies();
        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri, context.getPath());
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }

    }

    public boolean isForward(){return forward;}

    public void setForward(boolean forward){this.forward = forward;}

    public String getParameter(String name){
        String[] values = parameterMap.get(name);
        if(values != null && 0 != values.length)
            return values[0];
        return null;
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames(){
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameterMap. get(name);
    }

    public Connector getConnector(){return connector;}

    public HttpSession getSession() {
        return session;
    }

    public Socket getSocket(){return socket;}

    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public void setUri(String uri){
        this.uri = uri;
    }

    private void parseParameters(){
        if(getMethod().equals("GET")){
            String uri = StrUtil.subBetween(requestString," "," ");
            if(StrUtil.contains(uri,'?')){
                queryString = StrUtil.subAfter(uri,"?",false);
            }
        }
        if(getMethod().equals("POST")){
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if(queryString == null)
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameters = queryString.split("&");
        for(String parameter : parameters){
            String[] keyValuePair = parameter.split("=");
            String key = keyValuePair[0];
            String value = keyValuePair[1];
            String values[] = parameterMap.get(key);
            if(values == null){
                String[] input = new String[]{value};
                parameterMap.put(key,input);
            }else{
                values = ArrayUtil.append(values,value);
                parameterMap.put(key,values);
            }
        }
    }

    private void parseCookies(){
        String cookies = headerMap.get("cookie");
        List<Cookie> target = new ArrayList<>();
        if(cookies != null){
            String[] cookieList = cookies.split(";");
            for(String c : cookieList){
                if(StrUtil.isBlank(c))
                    continue;
                String[] pair = c.split("=");
                String name = pair[0].trim();
                String value = pair[1].trim();
                Cookie cookie = new Cookie(name,value);
                target.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(target,Cookie.class);
    }

    public String getHeader(String name){
        if(name == null)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    public Enumeration getHeaderNames(){
        return Collections.enumeration(headerMap.keySet());
    }

    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    public Cookie[] getCookies(){return cookies;}

    public void parseHeaders() {
        StringReader stringHeader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringHeader,lines);
        for(int i = 1; i < lines.size(); i++){
            String line = lines.get(i);
            if(line.length()==0)
                break;
            String[] headInfoPair = line.split(":");
            String name = headInfoPair[0].toLowerCase();
            String value = headInfoPair[1];
            headerMap.put(name,value);
        }
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    private void parseContext() {
        Engine engine = connector.getService().getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    // deal with the requestString, make its value equals the browser's request context.
    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
        requestString = new String(bytes, "utf-8");
    }

    // get the uri
    private void parseUri() {
        String temp;
        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public String getJSessionIdFromCookie(){
        if(cookies == null){
            return null;
        }
        for(Cookie cookie : cookies){
            if(cookie.getName().equals("JSESSIONID"))
                return cookie.getValue();
        }
        return null;
    }

    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    public String getProtocol() {
        return "HTTP:/1.1";
    }
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }
    public int getRemotePort() {
        return socket.getPort();
    }
    public String getScheme() {
        return "http";
    }
    public String getServerName() {
        return getHeader("host").trim();
    }
    public int getServerPort() {
        return getLocalPort();
    }
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    public String getRequestURI() {
        return uri;
    }
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }
    public String getServletPath() {
        return uri;
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }
}