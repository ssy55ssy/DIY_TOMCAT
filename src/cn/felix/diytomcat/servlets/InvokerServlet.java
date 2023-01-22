package cn.felix.diytomcat.servlets;

import cn.felix.diytomcat.catalina.Context;
import cn.felix.diytomcat.http.Request;
import cn.felix.diytomcat.http.Response;
import cn.felix.diytomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InvokerServlet extends HttpServlet {
	private static InvokerServlet instance = new InvokerServlet();

	public static synchronized InvokerServlet getInstance() {
		return instance;
	}

	private InvokerServlet() {
	}

	//  Obtain the ServletClassName according to the requested uri, then instantiate it, and then call its service method.
	public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
			Request request = (Request) httpServletRequest;
			Response response = (Response) httpServletResponse;
			String uri = request.getUri();
			Context context = request.getContext();
			String servletClassName = context.getServletClassName(uri);
			try {
				//When instantiating the servlet object,
				// use the context.getWebappClassLoader().loadClass() method to obtain the class object according to the class name,
				// and then instantiate the servlet object according to the class object.
				Class servletClass = context.getWebClassLoader().loadClass(servletClassName);
				Object servletObject = context.getServlet(servletClass);
				ReflectUtil.invoke(servletObject, "service", request, response);
				// if response has redirectPath, set the status to 302 & client jump
				if(response.getRedirectPath() != null){
					response.setStatus(Constant.CODE_302);
				}else{
					response.setStatus(Constant.CODE_200);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

	}
}
