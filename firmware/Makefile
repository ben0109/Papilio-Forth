COMPILER_FLAGS= -map memory.map -size 0x800
TESTFILES=forth/test/test-uart-tx.fs

memory.init memory.map: $(FILES)
	java -cp ./bin j1.Compiler -format init -output memory.init $(COMPILER_FLAGS) $(TESTFILES)

FILES=forth/core/base.fs forth/core/interactive.fs
# forth/core/compiler.fs
	
memory.mem: $(FILES)
	java -cp ./bin j1.Compiler -format mem -output memory.mem $(COMPILER_FLAGS) $(FILES)
