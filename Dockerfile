FROM ubuntu:latest
#FROM amazoncorretto:17
RUN apt-get update
RUN apt-get install -y wget
RUN apt-get update
RUN apt-get install -y gnupg
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN wget -O- https://apt.corretto.aws/corretto.key | apt-key add -
RUN add-apt-repository 'deb https://apt.corretto.aws stable main'
RUN apt-get update
RUN apt-get install -y java-17-amazon-corretto-jdk
RUN apt-get update
RUN apt-get install -y tesseract-ocr
WORKDIR /app
#ADD /target/ocr-crap.jar ocr-crap.jar
COPY ./target/creditstest.jar /app
COPY ./src/main/resources/tessdata/eng.traineddata /app
EXPOSE 8080
ENTRYPOINT ["java","-jar","creditstest.jar"]
#RUN yum install tesseract-ocr