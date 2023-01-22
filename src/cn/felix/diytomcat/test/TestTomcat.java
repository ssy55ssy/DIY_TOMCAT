package cn.felix.diytomcat.test;

import cn.felix.diytomcat.util.MiniBrowser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.net.www.content.text.plain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {
    private static int port = 18080;
    private static String ip = "127.0.0.1";
    @BeforeClass
    public static void beforeClass() {
        //check if the tomcat server has started before every test case
        if(NetUtil.isUsableLocalPort(port)) {
            System.err.println("please open the diy tomcat, which port is: " +port+ " or it can't work");
            System.exit(1);
        }
        else {
            System.out.println("diy tomcat has started, the test will start");
        }
    }

    // basic function test & test welcome page function
    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello DIY Tomcat from felix.cn");
    }

    // test the function of access text file
    @Test
    public void testaHtml() {
        String html = getContentString("/a.html");
        Assert.assertEquals(html,"Hello DIY Tomcat from a.html");
    }

    //test whether multi-thread is working, if tomcat is a single thread program, the time-consumption will be larger than 3 seconds
    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(10));
        TimeInterval timeInterval = DateUtil.timer();
        for(int i = 0; i<3; i++){
            threadPool.execute(new Runnable(){
                public void run() {
                    getContentString("/timeConsume.html");
                }
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
        long duration = timeInterval.intervalMs();
        Assert.assertTrue(duration < 3000);
    }

    // test multi applications
    @Test
    public void testaIndex_1() {
        String html = getContentString("/a/index.html");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@a");
    }

    // test whether tomcat can parse server.xml file and add context defined in it or not
    @Test
    public void testbIndex() {
        String html = getContentString("/b/");
        Assert.assertEquals(html,"Hello DIY Tomcat from index.html@b");
    }

    // test the 404 error code function
    @Test
    public void test404() {
        String response  = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }

    // test the 500 error code function
    @Test
    public void test500() {
        String response  = getHttpString("/500.html");
        containAssert(response, "HTTP/1.1 500 Internal Server Error");
    }

    //check that it returns Content-Type: text/plain, because txt files return it instead of text/html
    @Test
    public void testaTxt() {
        String response  = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }

    // test whether tomcat can response png file or not
    @Test
    public void testPNG() {
        byte[] bytes = getContentBytes("/logo.png");
        int pngFileLength = 3953;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    // test whether tomcat can response pdf file or not
    @Test
    public void testPDF() {
        byte[] bytes = getContentBytes("/etf.pdf");
        int pngFileLength = 3590775;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    // test the basic function of servlet & parse web.xml
    @Test
    public void testhello() {
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html,"Hello DIY Tomcat from HelloServlet");
    }


    /*

    caution : In order to test follow test cases, you should download javaweb project into D:/project
    you can find this project through https://github.com/ssy55ssy/javaweb.git

     */

    @Test
    public void testJavawebHello() {
        String html = getContentString("/javaweb/hello");
        containAssert(html,"Hello DIY Tomcat from HelloServlet@javaweb");
    }

    // test whether servelt in this tomcat is singleton
    @Test
    public void testJavawebHelloSingleton() {
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1,html2);
    }

    // test whether servlet can use getParameter() in doGet() method
    @Test
    public void testgetParam() {
        String uri = "/javaweb/param";
        String url = StrUtil. format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser. getContentString(url, params, true);
        Assert.assertEquals(html,"get name:meepo");
    }

    // test whether servlet can use getParameter() in doPost() method
    @Test
    public void testpostParam() {
        String uri = "/javaweb/param";
        String url = StrUtil. format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser. getContentString(url, params, false);
        Assert.assertEquals(html,"post name:meepo");
    }

    // test whether servlet can get header information
    @Test
    public void testheader() {
        String html = getContentString("/javaweb/header");
        Assert.assertEquals(html,"felix mini brower / java1.8");
    }

    // test whether servlet can set cookie
    @Test
    public void testsetCookie() {
        String html = getHttpString("/javaweb/setCookie");
        containAssert(html,"Set-Cookie: name=Gareen(cookie); Expires=");
    }

    // test whether servlet can get cookie
    @Test
    public void testgetCookie() throws IOException {
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/getCookie");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","name=Gareen(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containAssert(html,"name:Gareen(cookie)");
    }

    // test whether request can setSession and getSession
    @Test
    public void testSession() throws IOException {
        String jsessionid = getContentString("/javaweb/setSession");
        if(null!=jsessionid)
            jsessionid = jsessionid. trim();
        String url = StrUtil. format("http://{}:{}{}", ip, port,"/javaweb/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u. openConnection();
        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
        conn. connect();
        InputStream is = conn. getInputStream();
        String html = IoUtil. read(is, "utf-8");
        containAssert(html,"Gareen(session)");
    }

    // test the compress function
    @Test
    public void testGzip() {
        byte[] gzipContent = getContentBytes("/",true);
        byte[] unGzipContent = ZipUtil.unGzip(gzipContent);
        String html = new String(unGzipContent);
        Assert.assertEquals(html, "Hello DIY Tomcat from felix.cn");
    }

    @Test
    public void testJsp() {
        String html = getContentString("/javaweb/");
        Assert.assertEquals(html, "hello jsp@javaweb");
    }

    // test the client jump function
    @Test
    public void testClientJump(){
        String http_servlet = getHttpString("/javaweb/jump1");
        containAssert(http_servlet,"HTTP/1.1 302 Found");
        String http_jsp = getHttpString("/javaweb/jump1.jsp");
        containAssert(http_jsp,"HTTP/1.1 302 Found");
    }

    // test the server jump function
    @Test
    public void testServerJump(){
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet,"Hello DIY Tomcat from HelloServlet");
    }

    // test the server jump with attribute function
    @Test
    public void testServerJumpWithAttributes(){
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet,"Hello DIY Tomcat from HelloServlet@javaweb, the name is gareen");
    }

    // test static war file deployment
    @Test
    public void testJavaweb0Hello() {
        String html = getContentString("/javaweb0/hello");
        containAssert(html,"Hello DIY Tomcat from HelloServlet@javaweb");
    }

    // test dynamic war file deployment
    // save & delete javaweb1.war & javaweb1 folder first
    // start the tomcat and put the javawab1.war under the wabapp folder
    // then start the test case
    @Test
    public void testJavaweb1Hello() {
        String html = getContentString("/javaweb1/hello");
        containAssert(html,"Hello DIY Tomcat from HelloServlet@javaweb");
    }
    
    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }

    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,gzip,null,true);
    }

    // returns the string http response content
    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentString(url,null,true);
    }

    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url,null,true);
        return http;
    }

    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }
}