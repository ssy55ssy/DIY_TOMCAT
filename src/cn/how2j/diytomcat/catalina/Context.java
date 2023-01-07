package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.classloader.WebappClassLoader;
import cn.how2j.diytomcat.exception.WebConfigDuplicatedException;
import cn.how2j.diytomcat.http.ApplicationContext;
import cn.how2j.diytomcat.http.StandardFilterConfig;
import cn.how2j.diytomcat.http.StandardServletConfig;
import cn.how2j.diytomcat.util.ContextXMLUtil;
import cn.how2j.diytomcat.watcher.ContextFileChangeWatcher;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

public class Context {
    private String path;
    private String docBase;
    private File contextWebXmlFile;

    private Map<String, String> url_servletClassName;
    private Map<String, String> url_ServletName;
    private Map<String, String> servletName_className;
    private Map<String, String> className_servletName;
    private Map<String, Map<String, String>> servlet_className_init_params;

    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_FilterNames;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    private Map<String, Map<String, String>> filter_className_init_params;
    private Map<String,Filter> filterPool;

    private List<ServletContextListener> listenerPool;

    private WebappClassLoader webappClassLoader;

    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;

    private ServletContext servletContext;
    private Map<Class<?>, HttpServlet> servletPool;

    private List<String> loadOnStartupServletClassNames;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        TimeInterval timeInterval = DateUtil.timer();
        this.host = host;
        this.reloadable = reloadable;

        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        this.url_servletClassName = new HashMap<>();
        this.url_ServletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();

        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this. filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this. filter_className_init_params = new HashMap<>();
        this.filterPool = new HashMap<>();
        this.listenerPool = new ArrayList<>();
        this.servletContext = new ApplicationContext(this);
        this.loadOnStartupServletClassNames = new ArrayList<>();
        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        this.servletPool = new HashMap<>();

        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase,timeInterval.intervalMs());
    }

    public void reload() {
        host.reload(this);
    }

    private void deploy() {
        parseListener();
        init();

        if(reloadable){
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    private void init() {
        if (!contextWebXmlFile.exists())
            return;

        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);
        parseServletInitParams(d);
        parseLoadOnStartup(d);
        handleLoadOnStartup();
        parseFilterMapping(d);
        parseFilterInitParams(d);
        initFilter();
        fireEvent("init");
    }

    private void parseServletMapping(Document d) {
        // url_ServletName
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_ServletName.put(urlPattern, servletName);
        }
        // servletName_className / className_servletName
        Elements servletNameElements = d.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        // url_servletClassName
        Set<String> urls = url_ServletName.keySet();
        for (String url : urls) {
            String servletName = url_ServletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    public void parseFilterMapping(Document d) {
        //url_filterName;
        Elements uris = d.select("filter-mapping url-pattern");
        for(Element uri : uris){
            String uriName = uri.text();
            String filterName = uri.parent().select("filter-name").text();
            List<String> object = url_FilterNames.get(uriName);
            if(object == null){
                object = new ArrayList<>();
                url_FilterNames.put(uriName,object);
            }
            object.add(filterName);
        }
        //filterName_className;
        //className_filterName;
        Elements filters = d.select("filter");
        for(Element filter : filters){
            String name = filter.select("filter-name").first().text();
            String clazz = filter.select("filter-class").first().text();
            filterName_className.put(name,clazz);
            className_filterName.put(clazz,name);
        }
        //url_FilterClassNames;
        Set<String> uriNames = url_FilterNames.keySet();
        for(String uriName:uriNames){
            List<String> urlFilterNames = url_FilterNames.get(uriName);
            if(urlFilterNames == null){
                urlFilterNames = new ArrayList<>();
                url_FilterNames.put(uriName,urlFilterNames);
            }
            for(String urlFilterName : urlFilterNames){
                String className = filterName_className.get(urlFilterName);
                List<String> currentPair = url_filterClassName.get(uriName);
                if(currentPair == null){
                    currentPair = new ArrayList<>();
                    url_filterClassName.put(uriName,currentPair);
                }
                currentPair.add(className);
            }
        }
    }

    private void parseFilterInitParams(Document d) {
        //filter_className_init_params
        Elements filters = d.select("filter-class");
        for(Element filter : filters){
            String filterClassName = filter.text();
            Elements initParams = filter.parent().select("init-param");
            if(initParams.isEmpty())
                continue;
            Map<String,String> nameValuePair = new HashMap<>();
            for(Element initParam : initParams){
                String name = initParam.select("param-name").first().text();
                String value = initParam.select("param-value").first().text();
                nameValuePair.put(name,value);
            }
            filter_className_init_params.put(filterClassName,nameValuePair);
        }
    }

    private void initFilter(){
        try{
            Set<String> classNameSet = className_filterName.keySet();
            for(String className : classNameSet){
                Class clazz = this.getWebClassLoader().loadClass(className);
                String filterName = className_filterName.get(className);
                Map<String,String> initMap = filter_className_init_params.get(className);
                StandardFilterConfig standardFilterConfig = new StandardFilterConfig(servletContext,filterName,initMap);
                Filter filter = filterPool.get(className);
                if(filter == null){
                    filter = (Filter)ReflectUtil.newInstance(clazz);
                    filter.init(standardFilterConfig);
                    filterPool.put(className,filter);
                }
            }
        }catch(ClassNotFoundException|ServletException e){
            e.printStackTrace();
        }
    }

    private boolean match(String pattern, String uri) {
        if(pattern.equals(uri))
            return true;
        else if(pattern.equals("/*"))
            return true;
        else if(pattern.startsWith("/*")){
            String patternExtName = StrUtil.subAfter(pattern,".",false);
            String uriExtName = StrUtil.subAfter(uri,".",false);
            if(patternExtName.equals(uriExtName))
                return true;
        }
        return false;
    }

    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> result = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for(String pattern : patterns){
            if(match(pattern,uri)){
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterNameSet = new HashSet<>();
        for(String pattern:matchedPatterns){
            List<String> classNames = url_filterClassName.get(pattern);
            matchedFilterNameSet.addAll(classNames);
        }
        for(String className : matchedFilterNameSet){
            result.add(filterPool.get(className));
        }
        return result;
    }

    private void parseListener(){
        try{
            if(!contextWebXmlFile.exists())
                return;
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);
            Elements elements = d.select("listener-class");
            for(Element element:elements){
                String listenerName = element.text();
                Class clazz = webappClassLoader.loadClass(listenerName);
                ServletContextListener listener = (ServletContextListener)clazz.newInstance();
                this.listenerPool.add(listener);
            }
        }catch(ClassNotFoundException|InstantiationException|IllegalAccessException e){

        }
    }

    public void fireEvent(String methodName){
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for(ServletContextListener listener:listenerPool){
            if(methodName.equals("init")){
                listener.contextInitialized(event);
            }else if(methodName.equals("destroy")){
                listener.contextDestroyed(event);
            }
        }
    }

    private void parseLoadOnStartup(Document d){
        Elements elements = d.select("load-on-startup");
        for(Element element:elements){
            String servletClassName = element.parent().select("servlet-class").first().text();
            loadOnStartupServletClassNames.add(servletClassName);
        }
    }

    private void handleLoadOnStartup(){
        for(String servletClassName : loadOnStartupServletClassNames){
            try{
                Class<?> clazz = webappClassLoader.loadClass(servletClassName);
                getServlet(clazz);
            }catch(ClassNotFoundException | IllegalAccessException | ServletException | InstantiationException e){
                e.printStackTrace();
            }

        }
    }

    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        // 判断逻辑是放入一个集合，然后把集合排序之后看两临两个元素是否相同
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }

        Collections.sort(contents);

        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }

    }

    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);

        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebappClassLoader getWebClassLoader() {
        return webappClassLoader;
    }

    public void stop() {
        fireEvent("destroy");
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();

        destroyServlets();
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }


    public synchronized HttpServlet  getServlet(Class<?> clazz)
            throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);

        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            ServletContext servletContext = this.getServletContext();

            String className = clazz.getName();
            String servletName = className_servletName.get(className);

            Map<String, String> initParameters = servlet_className_init_params.get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);

            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
        }

        return servlet;
    }

    private void parseServletInitParams(Document d) {
        Elements servletClassNameElements = d.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();

            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;


            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            servlet_className_init_params.put(servletClassName, initParams);

        }

//		System.out.println("class_name_init_params:" + servlet_className_init_params);

    }
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

}
