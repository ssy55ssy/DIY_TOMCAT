package cn.felix.diytomcat.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

import cn.felix.diytomcat.catalina.Context;
import cn.felix.diytomcat.catalina.Host;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.LogFactory;

public class ContextFileChangeWatcher {

	private WatchMonitor monitor;
	private boolean stop = false;

	public ContextFileChangeWatcher(Context context) {
		// context.getDocBase() is the dictory which need to be watched
		// Integer.MAX_VALUE represents the depth of monitoring.
		this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
			private void dealWith(WatchEvent<?> event) {
				// First add synchronized synchronization. Because this is an asynchronous process, when the file changes,
				// many events will be sent. So we have to deal with events one by one,
				// otherwise the Context will be overloaded multiple times.
				synchronized (ContextFileChangeWatcher.class) {
					// Get the currently changed file or folder name
					String fileName = event.context().toString();
					// When stop, it means that it has been overloaded, and the message that comes later will not be ignored .
					if (stop)
						return;
					// only the changes in jar class and xml should be handled, and the others do not need to be restarted
					if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
						stop = true;
						LogFactory.get().info(ContextFileChangeWatcher.this + " detected some document in web applicatoin has been changed {} " , fileName);
						// call the reload method of context
						context.reload();
					}
				}
			}

			@Override
			public void onCreate(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

			@Override
			public void onModify(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

			@Override
			public void onDelete(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

			@Override
			public void onOverflow(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

		});

		this.monitor.setDaemon(true);
	}

	public void start() {
		monitor.start();
	}

	public void stop() {
		monitor.close();
	}
}
