OBJ:=target/scala-2.9.1.final/tdash_2.9.1-0.2.jar

.PHONY : clean compile_obj compile

all: compile_obj
	@echo done.

compile_obj:
	./sbt.sh package
	
compile: all
	rm -rf ../build/servlet/WEB-INF/
	mkdir -p ../build/servlet/WEB-INF/
	cp $(OBJ) ../build/lib/
	cp web_${DOMAIN}.xml ../build/servlet/WEB-INF/web.xml
	
clean:
	./sbt.sh clean
