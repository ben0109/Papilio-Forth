package simulator;

public interface Memory {

	short read(short address);

	void write(short address, short value);
}
