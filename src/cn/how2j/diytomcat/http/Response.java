package cn.how2j.diytomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Response extends BaseResponse {
    private StringWriter stringWriter;
    private PrintWriter writer;
    private String contentType;
    private byte[] body;
    private int status;
    private List<Cookie> cookies;
    private String redirectPath;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public PrintWriter getWriter() {
        return writer;
    }
    public byte[] getBody() throws UnsupportedEncodingException {
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    @Override
    public int getStatus() {
        return status;
    }

    public String getRedirectPath(){
        return redirectPath;
    }

    public void sendRedirect(String string) throws IOException{
        this.redirectPath = string;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }
    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public String getCookiesHeader() {
        if(null == cookies)
            return "";
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer sb = new StringBuffer();
        for(Cookie cookie:getCookies()){
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
            sb.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if(-1 != cookie.getMaxAge()){
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if(null != cookie.getPath()){
                sb.append("Path=" + cookie.getPath());
            }
        }
        return sb.toString();
    }


}