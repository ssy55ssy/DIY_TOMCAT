package cn.felix.diytomcat.catalina;

import cn.felix.diytomcat.util.Constant;
import cn.felix.diytomcat.util.ServerXMLUtil;
import cn.felix.diytomcat.watcher.WarFileWatcher;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;
    private Map<String, Context> contextMap;
    private WarFileWatcher warFileWatcher;
    private Engine engine;
    public Host(String name, Engine engine){
        this.contextMap = new HashMap<>();
        this.name =  name;
        this.engine = engine;
        this.warFileWatcher = new WarFileWatcher(this);
        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
        scanWarOnWebAppFolder();
        // start the war file watcher
        warFileWatcher.start();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // get the context through ServerXMLUtil, and put it into the contextMap.
    private  void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    //scan the directories under the webapps folder, and call loadContext to load these directories.
    private  void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }

    private void scanWarOnWebAppFolder(){
        File[] files = Constant.webappsFolder.listFiles();
        for(File file:files){
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            loadWar(file);
        }
    }

    public void loadWar(File warFile) {
        //get filename
        String filename = warFile.getName();
        //get foldername
        String folderName = StrUtil.subBefore(filename,".",true);
        //try to get context
        Context context = getContext("/" + folderName);
        if(context != null)
            return;
        //try to get existed folder
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;
        //move war file
        File targetFile = FileUtil.file(Constant.webappsFolder,folderName,filename);
        File targetFolder = targetFile.getParentFile();
        targetFolder.mkdir();
        FileUtil.copyFile(warFile,targetFile);
        //unzip
        String command = "jar xvf " + filename;
        Process process = RuntimeUtil.exec(null,targetFolder,command);
        try{
            process.waitFor();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        //delete war
        FileUtil.del(targetFile);
        //create context
        loadContext(targetFolder);
    }

    private  void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this, true);

        contextMap.put(context.getPath(), context);
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    //this method is related to hot load. when the monitor of context detected some file in web-inf has been changed,
    // this function will be called
    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        // save some basic info of context
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // stop context's watcher
        context.stop();
        // remove
        contextMap.remove(path);
        // allocate new context
        Context newContext = new Context(path, docBase, this, reloadable);
        // assign it to map
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());

    }
}
