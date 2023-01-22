package cn.felix.diytomcat.catalina;

import cn.felix.diytomcat.http.Request;
import cn.felix.diytomcat.http.Response;
import cn.felix.diytomcat.http.StandardSession;
import cn.felix.diytomcat.servlets.DefaultServlet;
import cn.felix.diytomcat.servlets.InvokerServlet;
import cn.felix.diytomcat.servlets.JspServlet;
import cn.felix.diytomcat.util.Constant;
import cn.felix.diytomcat.util.SessionManager;
import cn.felix.diytomcat.util.WebXMLUtil;
import cn.felix.diytomcat.webappservlet.HelloServlet;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Session;
import cn.hutool.log.LogFactory;
import org.apache.el.util.ReflectionUtil;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class HttpProcessor {
    public void execute(Socket s, Request request, Response response){
        try {
            String uri = request.getUri();
            if(null==uri)
                return;
            Context context = request.getContext();
            // Obtain servletClassName through context, if it is empty, it means that it is not an accessed servlet.
            String servletClassName = context.getServletClassName(uri);
            // prepare session. get SessionId from cookie in request, then find the corresponding session in the sessionManager
            prepareSession(request,response);
            HttpServlet workingServlet;
            if(null!=servletClassName)
                workingServlet = InvokerServlet.getInstance();
            else if(uri.endsWith(".jsp")){
                workingServlet = JspServlet.getInstance();
            }
            else
                workingServlet = DefaultServlet.getInstance();
            // get filters which matches the uri
            List<Filter> filterList = context.getMatchedFilters(uri);
            // create a filter chain. pass the working servlet to it, working servlet will be executed int the end
            ApplicationFilterChain chain = new ApplicationFilterChain(filterList,workingServlet);
            chain.doFilter(request,response);
            // if request's forward is true, it shouldn't be processed later, since the redirected uri should be processed
            if(request.isForward())
                return;
            if(Constant.CODE_200 == response.getStatus()){
                handle200(s, response,request);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()){
                handle302(s, response,request);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(s, uri);
                return;
            }

        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(s,e);
        }
        finally{
            // whether success or not, close the socket
            try {
                if(!s.isClosed())
                    s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void prepareSession(Request request, Response response){
        String sessionId = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(sessionId,request,response);
        request.setSession(session);
    }

    private static void handle200(Socket s, Response response,Request request) throws IOException {
        String contentType = response.getContentType();
        // whether  compress or not
        boolean ifGzip = isGzip(request,response.getBody(),contentType);
        String headText = null;
        if(ifGzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType,response.getCookiesHeader());
        byte[] head = headText.getBytes();
        byte[] body = response.getBody();
        if(ifGzip)
            // use the ZipUtil to compress the body
            body = ZipUtil.gzip(body);
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);
        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
    }

    private void handle302(Socket s, Response response,Request request) throws IOException {
        String headText = Constant.response_head_302;
        headText = StrUtil.format(headText,response.getRedirectPath());
        byte[] responseBytes = headText.getBytes();
        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
    }

    private void handle404(Socket s, String uri) throws IOException {
        OutputStream os = s.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes("utf-8");
        os.write(responseByte);
    }

    private void handle500(Socket s, Exception e) {
        try {
            OutputStream os = s.getOutputStream();
            //Get the exception stack of Exception
            StackTraceElement stes[] = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement ste : stes) {
                sb.append("\t");
                sb.append(ste.toString());
                sb.append("\r\n");
            }
            String msg = e.getMessage();
            //Sometimes the message is too long, more than 20, truncated to facilitate display
            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);
            //Combining the header and text templates to form an http response and send it out
            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), sb.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes("utf-8");
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // determine whether to compress
    private static boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings=  request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;
        Connector connector = request.getConnector();
        String compression = connector.getCompression();
        int compressionMinSize = connector.getCompressionMinSize();
        String noCompressionUserAgents = connector.getNoCompressionUserAgents();
        String compressableMimeType = connector.getCompressableMimeType();
        if(!compression.equals("on"))
            return false;
        if(body.length < compressionMinSize)
            return false;
        String[] agents = noCompressionUserAgents.split(",");
        for(String agent:agents){
            if(StrUtil.containsAny(request.getHeader("User-Agent"),agent.trim()))
                return false;
        }
        String[] mimeTypes = compressableMimeType.split(",");
        for(String type : mimeTypes){
            type = type.trim();
            if(type.equals(mimeType))
                return true;
        }
        return true;
    }

}
