package cn.how2j.diytomcat.classloader;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class JspClassLoader extends URLClassLoader{

    private static Map<String,JspClassLoader> map = new HashMap<>();

    public static void invalidJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

    public static JspClassLoader getJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        JspClassLoader jspClassLoader = map.get(key);
        if(jspClassLoader == null){
            jspClassLoader = new JspClassLoader(context);
            map.put(key,jspClassLoader);
        }
        return jspClassLoader;
    }

    private JspClassLoader(Context context){
        super(new URL[] {}, context.getWebClassLoader());
        try {
            String subFolder;
            String path = context.getPath();
            if ("/".equals(path))
                subFolder = "_";
            else
                subFolder = StrUtil.subAfter(path, '/', false);
            File classesFolder = new File(Constant.workFolder, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
