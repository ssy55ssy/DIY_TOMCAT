/*
 * Generated by the Jasper component of Apache Tomcat
 * Version: JspCServletContext/1.0
 * Generated at: 2022-12-13 06:08:41 UTC
 * Note: The last modified time of this file was set to
 *       the last modified time of the source file after
 *       generation to assist with modification tracking.
 */
package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class try_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final javax.servlet.jsp.JspFactory _jspxFactory =
          javax.servlet.jsp.JspFactory.getDefaultFactory();

  private static java.util.Map<java.lang.String,java.lang.Long> _jspx_dependants;

  private volatile javax.el.ExpressionFactory _el_expressionfactory;
  private volatile org.apache.tomcat.InstanceManager _jsp_instancemanager;

  public java.util.Map<java.lang.String,java.lang.Long> getDependants() {
    return _jspx_dependants;
  }

  public javax.el.ExpressionFactory _jsp_getExpressionFactory() {
    if (_el_expressionfactory == null) {
      synchronized (this) {
        if (_el_expressionfactory == null) {
          _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        }
      }
    }
    return _el_expressionfactory;
  }

  public org.apache.tomcat.InstanceManager _jsp_getInstanceManager() {
    if (_jsp_instancemanager == null) {
      synchronized (this) {
        if (_jsp_instancemanager == null) {
          _jsp_instancemanager = org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig());
        }
      }
    }
    return _jsp_instancemanager;
  }

  public void _jspInit() {
  }

  public void _jspDestroy() {
  }

  public void _jspService(final javax.servlet.http.HttpServletRequest request, final javax.servlet.http.HttpServletResponse response)
        throws java.io.IOException, javax.servlet.ServletException {

    final javax.servlet.jsp.PageContext pageContext;
    javax.servlet.http.HttpSession session = null;
    final javax.servlet.ServletContext application;
    final javax.servlet.ServletConfig config;
    javax.servlet.jsp.JspWriter out = null;
    final java.lang.Object page = this;
    javax.servlet.jsp.JspWriter _jspx_out = null;
    javax.servlet.jsp.PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html; charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\r\n\r\n\r\n<script>\r\n    $(function(){\r\n        <c:if test=\"");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${!empty msg}", java.lang.String.class, (javax.servlet.jsp.PageContext)_jspx_page_context, null, false));
      out.write("\">\r\n        $(\"span.errorMessage\").html(\"");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg}", java.lang.String.class, (javax.servlet.jsp.PageContext)_jspx_page_context, null, false));
      out.write("\");\r\n        $(\"div.loginErrorMessageDiv\").show();\r\n        </c:if>\r\n        $(\"form.loginForm\").submit(function(){\r\n            if(0==$(\"#name\").val().length||0==$(\"#password\").val().length){\r\n                $(\"span.errorMessage\").html(\"请输入账号密码\");\r\n                $(\"div.loginErrorMessageDiv\").show();\r\n                return false;\r\n            }\r\n            return true;\r\n        });\r\n\r\n        $(\"form.loginForm input\").keyup(function(){\r\n            $(\"div.loginErrorMessageDiv\").hide();\r\n        });\r\n\r\n\r\n\r\n        var left = window.innerWidth/2+162;\r\n        $(\"div.loginSmallDiv\").css(\"left\",left);\r\n    })\r\n</script>\r\n\r\n\r\n<div id=\"loginDiv\" style=\"position: relative\">\r\n\r\n    <form class=\"loginForm\" action=\"forelogin\" method=\"post\">\r\n        <div id=\"loginSmallDiv\" class=\"loginSmallDiv\">\r\n            <div class=\"loginErrorMessageDiv\">\r\n                <div class=\"alert alert-danger\" >\r\n                    <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"></button>\r\n                    <span class=\"errorMessage\"></span>\r\n");
      out.write("                </div>\r\n            </div>\r\n\r\n            <div class=\"login_acount_text\">账户</div>\r\n            <div class=\"loginInput \" >\r\n\t\t\t\t<span class=\"loginInputIcon \">\r\n\t\t\t\t\t<span class=\" glyphicon glyphicon-user\"></span>\r\n\t\t\t\t</span>\r\n                <input id=\"name\" name=\"name\" placeholder=\"手机/会员名/邮箱\" type=\"text\">\r\n            </div>\r\n\r\n            <div class=\"loginInput \" >\r\n\t\t\t\t<span class=\"loginInputIcon \">\r\n\t\t\t\t\t<span class=\" glyphicon glyphicon-lock\"></span>\r\n\t\t\t\t</span>\r\n                <input id=\"password\" name=\"password\" type=\"password\" placeholder=\"密码\" type=\"text\">\r\n            </div>\r\n            <span class=\"text-danger\">不要输入真实的天猫账号密码</span><br><br>\r\n\r\n\r\n            <div>\r\n                <a class=\"notImplementLink\" href=\"#nowhere\">忘记登录密码</a>\r\n                <a href=\"register.jsp\" class=\"pull-right\">免费注册</a>\r\n            </div>\r\n            <div style=\"margin-top:20px\">\r\n                <button class=\"btn btn-block redButton\" type=\"submit\">登录</button>\r\n            </div>\r\n        </div>\r\n    </form>\r\n");
      out.write("\r\n\r\n</div>");
    } catch (java.lang.Throwable t) {
      if (!(t instanceof javax.servlet.jsp.SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try {
            if (response.isCommitted()) {
              out.flush();
            } else {
              out.clearBuffer();
            }
          } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
        else throw new ServletException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
