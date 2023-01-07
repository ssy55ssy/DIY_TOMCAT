package cn.how2j.diytomcat.servlets;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.classloader.JspClassLoader;
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.JspUtil;
import cn.how2j.diytomcat.util.WebXMLUtil;
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

public class JspServlet extends HttpServlet {
    private static JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return instance;
    }

    private JspServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        try{
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            Context context = request.getContext();

            String uri = request.getUri();
            if ("/500.html".equals(uri))
                throw new RuntimeException("this is a deliberately created exception");

            if ("/".equals(uri))
                uri = WebXMLUtil.getWelcomeFile(request.getContext());

            String fileName = StrUtil.removePrefix(uri, "/");

            File file = FileUtil.file(request.getRealPath(fileName));

            if (file.exists()) {
                String path = context.getPath();
                String subFolder;
                if ("/".equals(path))
                    subFolder = "_";
                else
                    subFolder = StrUtil.subAfter(path, '/', false);
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                File jspServletClassFile = new File(servletClassPath);
                if (!jspServletClassFile.exists()) {
                    JspUtil.compileJsp(context, file);
                } else if (file.lastModified() > jspServletClassFile.lastModified()) {
                    JspClassLoader.invalidJspClassLoader(uri,context);
                    JspUtil.compileJsp(context, file);
                }
                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);
                JspClassLoader classLoader = JspClassLoader.getJspClassLoader(uri,context);
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                Class jspServletClass = classLoader.loadClass(jspServletClassName);
                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request,response);
//                byte body[] = FileUtil.readBytes(file);
//                response.setBody(body);
                if(response.getRedirectPath() != null){
                    response.setStatus(Constant.CODE_302);
                }else{
                    response.setStatus(Constant.CODE_200);
                }
            } else {
                response.setStatus(Constant.CODE_404);
            }
        }catch(ClassNotFoundException|IllegalAccessException|InstantiationException e){
            e.printStackTrace();
        }

    }


}
