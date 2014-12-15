
IMAGES=memory.init memory.map memory.mem memory.hex
SOURCES=$(wildcard ./src/compiler/*.java ./src/j1/*.java ./src/simulator/*.java ./src/*.java)
TOOLS=$(subst ./src,./bin,$(SOURCES:.java=.class))

COMPILER_FLAGS= -map memory.map -size 0x800
TESTFILES=forth/test/test-uart-tx.fs

all: $(IMAGES)
	
tools: $(TOOLS)
images: $(IMAGES)

memory.init memory.map: $(FILES) $(TOOLS)
	java -cp ./bin j1.Compiler -format init -output memory.init $(COMPILER_FLAGS) $(TESTFILES)

FILES=forth/core/base.fs forth/core/interactive.fs
# forth/core/compiler.fs
	
memory.mem: $(FILES) $(TOOLS)
	java -cp ./bin j1.Compiler -format mem -output memory.mem $(COMPILER_FLAGS) $(FILES)

memory.hex: $(FILES) $(TOOLS)
	java -cp ./bin j1.Compiler -format hex -output memory.hex $(COMPILER_FLAGS) $(FILES)


./bin/%.class: src/%.java
	javac -cp ./src -d ./bin $<

./bin/compiler/%.class: src/compiler/%.java
	javac -cp ./src -d ./bin $<
	
./bin/j1/%.class: src/j1/%.java
	javac -cp ./src -d ./bin $<

./bin/simulator/%.class: src/j1/%.java
	javac -cp ./src -d ./bin $<
	
clean:
	rm -f ./bin/compiler/*.class ./bin/j1/*.class ./bin/simulator/*.class ./bin/*.class
	rm -f $(IMAGES)