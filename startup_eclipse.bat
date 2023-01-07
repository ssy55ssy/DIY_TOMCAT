del /q bootstrap.jar
jar cvf0 bootstrap.jar -C bin cn/how2j/diytomcat/Bootstrap.class -C bin cn/how2j/diytomcat/classloader/CommonClassLoader.class
del /q lib/diytomcat.jar
cd bin
jar cvf0 ../lib/diytomcat.jar *
cd ..
java -cp bootstrap.jar cn.how2j.diytomcat.Bootstrap 
pause