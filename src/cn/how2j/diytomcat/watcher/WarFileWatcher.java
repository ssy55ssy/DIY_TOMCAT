package cn.how2j.diytomcat.watcher;

import cn.how2j.diytomcat.catalina.Host;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static cn.hutool.core.io.watch.WatchMonitor.ENTRY_CREATE;

public class WarFileWatcher {
    private WatchMonitor monitor;

    public WarFileWatcher(Host host){
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {

            private void dealWith(WatchEvent<?> watchEvent, Path path){
                synchronized (WarFileWatcher.class){
                    String fileName = watchEvent.context().toString();
                    if(fileName.toLowerCase().endsWith(".war") && ENTRY_CREATE.equals(watchEvent.kind())){
                        File warFile = FileUtil.file(Constant.webappsFolder,fileName);
                        host.loadWar(warFile);
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent,path);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent,path);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent,path);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent,path);
            }
        });
    }

    public void start(){
        monitor.start();
    }

    public void end(){
        monitor.interrupt();
    }

}
