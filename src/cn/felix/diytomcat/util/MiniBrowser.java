package cn.felix.diytomcat.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;

/*
intimate a browser, which sends request to the host.
key parameters:
1.isGet. If true, the browser will use the get method, otherwise it will use the post method
2.params. Parameters which need to be sent to the host
3.gzip. If can accept zipped response.
 */

public class MiniBrowser {

    public static void main(String[] args) throws Exception {
    }

    //returns the binary http response content
    public static byte[] getContentBytes(String url,Map<String,Object> params, boolean isGet) {
        return getContentBytes(url, false,params,isGet);
    }

    //returns the string http response content
    public static String getContentString(String url,Map<String,Object> params, boolean isGet) {
        return getContentString(url,false,params,isGet);
    }

    //returns the string http response content
    public static String getContentString(String url, boolean gzip,Map<String,Object> params, boolean isGet) {
        byte[] result = getContentBytes(url, gzip,params,isGet);
        if(null==result)
            return null;
        try {
            return new String(result,"utf-8").trim();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    //returns the binary http response content
    public static byte[] getContentBytes(String url, boolean gzip,Map<String,Object> params, boolean isGet) {
        byte[] response = getHttpBytes(url,gzip,params,isGet);
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;
        for (int i = 0; i < response.length-doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);

            if(Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if(-1==pos)
            return null;

        pos += doubleReturn.length;

        byte[] result = Arrays.copyOfRange(response, pos, response.length);
        return result;
    }

    // return the string http response
    public static String getHttpString(String url,boolean gzip,Map<String,Object> params, boolean isGet) {
        byte[]  bytes=getHttpBytes(url,gzip,params,isGet);
        return new String(bytes).trim();
    }

    // return the string http response
    public static String getHttpString(String url,Map<String,Object> params, boolean isGet) {
        return getHttpString(url,false,params,isGet);
    }

    // return the binary http response
    public static byte[] getHttpBytes(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        String method = isGet ? "GET" : "POST";
        byte[] result = null;
        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if(-1==port)
                port = 80;
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            client.connect(inetSocketAddress, 1000);
            Map<String,String> requestHeaders = new HashMap<>();

            requestHeaders.put("Host", u.getHost()+":"+port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "felix mini browser / java1.8");

            if(gzip)
                requestHeaders.put("Accept-Encoding", "gzip");

            String path = u.getPath();
            if(method.equals("GET") && params != null){
                String p = HttpUtil.toParams(params);
                path = path + "?" + p;
            }
            if(path.length()==0)
                path = "/";

            String firstLine = method + " " + path + " HTTP/1.1\r\n";

            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+"\r\n";
                httpRequestString.append(headerLine);
            }
            if(method.equals("POST") && params != null){
                String p = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(p);
            }
            PrintWriter pWriter = new PrintWriter(client.getOutputStream(), true);
            pWriter.println(httpRequestString);

            InputStream is = client.getInputStream();
            result = readBytes(is,true);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result = e.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }

        return result;

    }

    // fully : if fully is true, ByteArrayOutputStream will still write buffer, even if the read data is not as long as buffersize.
    // The main purpose is to test the etf.pdf file. This file is relatively large, so during the transmission process, it may not transmit 1024
    // bytes at a time, and sometimes it will be less than this number of bytes. If read when the received data is less than this byte,
    // then the read file will be incomplete.
    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        int buffer_size = 1024;
        byte buffer[] = new byte[buffer_size];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(true) {
            int length = is.read(buffer);
            if(-1==length)
                break;
            baos.write(buffer, 0, length);
            if(!fully && length!=buffer_size)
                    break;
        }
        byte[] result =baos.toByteArray();
        return result;
    }
}