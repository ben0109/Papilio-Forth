source:=1-stack 2-integer 3-comparison 4-memory 5-control 6-output 7-dictionary 8-input 9-compiler
source-files:=$(source:%=forth/core/%.fth)

memory.bin memory.mem memory.map: $(source-files)
	java -cp ./bin compiler.Compiler > compiler.log

