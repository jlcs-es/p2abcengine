
FROM openjdk:7


COPY idemix-3.0.36-binaries/ /idemix/

RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

RUN cd /idemix/com/ibm/zurich/idmx/com.ibm.zurich.idmx.3_x_x/3.0.36                 && \
    mvn install:install-file                                                           \
   -Dfile=com.ibm.zurich.idmx.3_x_x-3.0.36.jar                                         \
   -DpomFile=com.ibm.zurich.idmx.3_x_x-3.0.36.pom                                   && \
cd /idemix/com/ibm/zurich/idmx/com.ibm.zurich.idmx.interfaces/3.0.36                && \
    mvn install:install-file                                                           \
   -Dfile=com.ibm.zurich.idmx.interfaces-3.0.36.jar                                    \
   -DpomFile=com.ibm.zurich.idmx.interfaces-3.0.36.pom                              && \
cd /idemix/com/ibm/zurich/idmx/pom/3.0.36                                           && \
    mvn install:install-file                                                           \
   -Dfile=pom-3.0.36.pom                                                               \
   -DpomFile=pom-3.0.36.pom
   
   
# docker run -it --name p2abce_dev_env -v /home/joseluis/GitHub/p2abcengine/:/usr/src/mymaven -w /usr/src/mymaven/Code/core-abce p2abce_env bash

# mvn clean install -DskipTests

# docker start p2abce_dev_env
# docker attach p2abce_dev_env
