package cn.felix.diytomcat.servlets;

import cn.felix.diytomcat.catalina.Context;
import cn.felix.diytomcat.http.Request;
import cn.felix.diytomcat.http.Response;
import cn.felix.diytomcat.util.Constant;
import cn.felix.diytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

// defaultServlet is created to process static resources
public class DefaultServlet extends HttpServlet {
    private static DefaultServlet instance = new DefaultServlet();

    public static synchronized DefaultServlet getInstance() {
        return instance;
    }

    private DefaultServlet() {
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        String uri = request.getUri();
        // this case is written to test if 500 error code function is work
        if ("/500.html".equals(uri))
            throw new RuntimeException("this is a deliberately created exception");
        // if rui == /, use the welcomeFile
        if ("/".equals(uri))
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
        // use the jspservlet if the uri is end with jsp
        if(uri.endsWith(".jsp")){
            JspServlet.getInstance().service(request,response);
            return;
        }
        String fileName = StrUtil.removePrefix(uri, "/");
        File file = FileUtil.file(request.getRealPath(fileName));
        if (file.exists()) {
            String extName = FileUtil.extName(file);
            String mimeType = WebXMLUtil.getMimeType(extName);
            response.setContentType(mimeType);
            byte body[] = FileUtil.readBytes(file);
            response.setBody(body);
            // this case is used to test time consumption in multi-thread
            if (fileName.equals("timeConsume.html"))
                ThreadUtil.sleep(1000);
            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(Constant.CODE_404);
        }

    }


}
