package cn.felix.diytomcat.test;

public class TestClassLoader {

	public static void main(String[] args) {

		System.out.println(Object.class.getClassLoader());
		
		System.out.println(TestClassLoader.class.getClassLoader());
		
	}
}
