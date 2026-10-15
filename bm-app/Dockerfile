FROM eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64
ENV TZ=Asia/Shanghai JAVA_OPTS="" PARAMS=""
COPY target/bm-app.jar /bm-app.jar
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /bm-app.jar $PARAMS"]
