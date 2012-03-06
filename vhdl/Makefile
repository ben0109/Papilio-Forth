MEM_PATH:=../firmware
ISE_PATH:=/home/ben/prog/Xilinx/13.4/ISE_DS/ISE
DATA2MEM:=$(ISE_PATH)/bin/lin/data2mem

MEM_FILE:=$(MEM_PATH)/memory.mem

load: final.bit
	sudo papilio-prog -v -f final.bit

final.bit: main.bit forth.bmm $(MEM_FILE)
	$(DATA2MEM) -bm forth.bmm -bd $(MEM_FILE) -bt main.bit -o b final.bit
