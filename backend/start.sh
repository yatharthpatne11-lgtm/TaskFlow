#!/bin/sh
# Make Tomcat listen on Render's $PORT and disable the internal shutdown port (8005)
sed -i "s/Connector port=\"8080\"/Connector port=\"${PORT:-8080}\"/" /usr/local/tomcat/conf/server.xml
sed -i 's/<Server port="8005"/<Server port="-1"/' /usr/local/tomcat/conf/server.xml
exec catalina.sh run