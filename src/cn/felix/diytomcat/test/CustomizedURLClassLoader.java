package cn.felix.diytomcat.test;

import java.net.URL;
import java.net.URLClassLoader;

public class CustomizedURLClassLoader extends URLClassLoader {

	public CustomizedURLClassLoader(URL[] urls) {
		super(urls);
	}

	public static void main(String[] args) throws Exception {
		URL url = new URL("file:d:/project/diytomcat/jar_4_test/test.jar");
		URL[] urls = new URL[] {url};
		
		CustomizedURLClassLoader loader1 = new CustomizedURLClassLoader(urls);
		Class<?> felixClass1 = loader1.loadClass("cn.felix.diytomcat.test.felix");

		CustomizedURLClassLoader loader2 = new CustomizedURLClassLoader(urls);
		Class<?> felixClass2 = loader2.loadClass("cn.felix.diytomcat.test.felix");

		System.out.println(felixClass1==felixClass2);

	}

}
