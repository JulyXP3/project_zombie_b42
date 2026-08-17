@echo off
rem Please change JAVA_HOME below to the JDK installation path on your local machine (e.g., E:JDK25).
set JAVA_HOME=E:\JDK25
.\gradlew.bat clean jar --console=plain -x test