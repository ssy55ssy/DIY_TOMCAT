package cn.how2j.diytomcat.util;

import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.http.StandardSession;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SessionManager {
    private static Map<String,StandardSession> sessionMap = new HashMap<>();
    private static int defaultTimeOut = getTimeOut();
    static{
        startSessionOutdateCheckThread();
    }

    private static int getTimeOut(){
        int defaultTimeOut = 30;
        try{
            Document d = Jsoup.parse(Constant.webXmlFile,"utf-8");
            Element e = d.select("session-timeout").first();
            if(e == null)
                return defaultTimeOut;
            return Integer.parseInt(e.text());
        }catch(IOException e){
            return defaultTimeOut;
        }
    }

    private static void startSessionOutdateCheckThread(){
        Thread t = new Thread(){
            public void run(){
                while(true){
                    checkOutDateSession();
                    ThreadUtil.sleep(30 * 1000);
                }
            }
        };
        t.start();
    }

    private static void checkOutDateSession(){
        List<String> expiredSessions = new ArrayList<>();
        Set<String> keySet = sessionMap.keySet();
        for(String sessionId : keySet){
            StandardSession session = sessionMap.get(sessionId);
            Long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            if(interval > defaultTimeOut * 1000)
                expiredSessions.add(sessionId);
        }
        for(String id : expiredSessions)
            sessionMap.remove(id);
    }

    public static synchronized String generateSessionId() {
        String result = null;
        byte[] bytes = RandomUtil. randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }

    public static HttpSession getSession(String jsessionid, Request request, Response response){
        if(jsessionid == null)
            return newSession(request,response);
        StandardSession cur = sessionMap.get(jsessionid);
        if(cur == null){
            return newSession(request,response);
        }else{
            cur.setLastAccessedTime(System.currentTimeMillis());
            createCookieBySession(cur,request,response);
            return cur;
        }
    }

    private static HttpSession newSession(Request request, Response response){
        String id = generateSessionId();
        StandardSession standardSession = new StandardSession(id,request.getServletContext());
        standardSession.setLastAccessedTime(System.currentTimeMillis());
        standardSession.setMaxInactiveInterval(defaultTimeOut);
        sessionMap.put(id,standardSession);
        createCookieBySession(standardSession, request, response);
        return standardSession;
    }

    private static void createCookieBySession(HttpSession session, Request request, Response response){
        Cookie cookie = new Cookie("JSESSIONID",session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

}
