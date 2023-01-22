package cn.felix.diytomcat.http;

import cn.felix.diytomcat.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class ApplicationRequestDispatcher implements RequestDispatcher {

    private String uri;

    public ApplicationRequestDispatcher(String uri){
        if(!uri.startsWith("/"))
            uri = "/" + uri;
        this.uri = uri;
    }

    // modify the uri of the request, and then execute it again through the execute of HttpProcessor
    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
            Request request = (Request) servletRequest;
            Response response = (Response) servletResponse;
            request.setUri(uri);
            HttpProcessor hp = new HttpProcessor();
            hp.execute(request.getSocket(),request,response);
            request.setForward(true);
    }

    @Override
    public void include(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
        // TODO Auto-generated method stub

    }
}
