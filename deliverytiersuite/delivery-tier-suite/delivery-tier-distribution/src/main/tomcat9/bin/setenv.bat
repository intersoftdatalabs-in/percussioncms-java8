SET JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager @JAVA_OPTS@
SET CLASSPATH=%CATALINA_BASE%/log4j2/lib/*;%CATALINA_BASE%/log4j2/conf;%CLASSPATH%

REM Suppress noisy "Ignoring call to j.u.l.Logger.setUseParentHandlers(true)" warnings
REM from the Log4j JUL bridge during early Tomcat startup.
SET JAVA_OPTS=%JAVA_OPTS% -Dorg.apache.logging.log4j.jul.Log4jLogger.level=OFF

REM CVE-2026-114938 (GH-288): proactive defense-in-depth for unpatched JDK 17/21+ (safe no-op on JDK 8).
SET CATALINA_OPTS=-XX:+IgnoreUnrecognizedVMOptions -XX:-UseBASE64Intrinsics %CATALINA_OPTS%



