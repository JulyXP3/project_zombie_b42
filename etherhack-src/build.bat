@echo off
rem 请将下面的 JAVA_HOME 改为你本机 JDK 的安装路径 (例如 E:\JDK17)
set JAVA_HOME=E:\JDK25
.\gradlew.bat clean jar --console=plain -x test