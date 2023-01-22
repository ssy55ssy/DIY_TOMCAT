package cn.felix.diytomcat.servlets;

import cn.felix.diytomcat.catalina.Context;
import cn.felix.diytomcat.classloader.JspClassLoader;
import cn.felix.diytomcat.http.Request;
import cn.felix.diytomcat.http.Response;
import cn.felix.diytomcat.util.Constant;
import cn.felix.diytomcat.util.JspUtil;
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

public class JspServlet extends HttpServlet {
    private static JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return instance;
    }

    private JspServlet() {
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
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
                // if the class file don't exist, compile a class file
                if (!jspServletClassFile.exists()) {
                    JspUtil.compileJsp(context, file);
                }
                // if the generate time of jsp file is later than the generate time of class
                // it means jsp file has been modified, so compile again
                else if (file.lastModified() > jspServletClassFile.lastModified()) {
                    JspClassLoader.invalidJspClassLoader(uri,context);
                    JspUtil.compileJsp(context, file);
                }
                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);
                // Get the JspClassLoader corresponding to the current jsp according to uri and context
                JspClassLoader classLoader = JspClassLoader.getJspClassLoader(uri,context);
                // Get the servlet Class Name corresponding to the jsp
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                // Load the class object according to the servlet Class Namethrough JspClassLoader: jspServletClass
                Class jspServletClass = classLoader.loadClass(jspServletClassName);
                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request,response);
                // if response has redirectPath, set the status to 302 & client jump
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
