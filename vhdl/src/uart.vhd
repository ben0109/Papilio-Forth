library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity uart is
	port (
		clk:		in  STD_LOGIC;
		rx:		in  STD_LOGIC;
		tx:		out STD_LOGIC;
		io_rd:	in  STD_LOGIC;
		io_wr:	in  STD_LOGIC;
		io_addr:	in  STD_LOGIC;
		io_din: 	in  STD_LOGIC_VECTOR (15 downto 0);
		io_dout:	out STD_LOGIC_VECTOR (15 downto 0));
end uart;

architecture Behavioral of uart is

	component uart_rx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		serial_in:	in  STD_LOGIC;
		rd:			in  STD_LOGIC;
		ready:		out STD_LOGIC;
		dout:			out STD_LOGIC_VECTOR (7 downto 0));
	end component;

	component uart_tx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		din:			in  STD_LOGIC_VECTOR (7 downto 0);
		wr:			in  STD_LOGIC;
		ready:		out STD_LOGIC;
		serial_out:	out STD_LOGIC);
	end component;

	signal clk_divider:	std_logic_vector(15 downto 0) := (others=>'0');
	
	signal rx_rd:			std_logic;
	signal rx_ready:		std_logic;
	signal rx_dout:		std_logic_vector(7 downto 0);
	
	signal tx_wr:			std_logic;
	signal tx_ready:		std_logic;
	
begin

	uart_rx_inst: uart_rx
	port map (
		clk				=> clk,
		clk_divider		=> clk_divider,
		serial_in		=> rx,
		rd					=> rx_rd,
		ready				=> rx_ready,
		dout				=> rx_dout);

	uart_tx_inst: uart_tx
	port map (
		clk				=> clk,
		clk_divider		=> clk_divider,
		din				=> io_din(7 downto 0),
		wr					=> tx_wr,
		ready				=> tx_ready,
		serial_out		=> tx);
	
	process (clk)
	begin
		if rising_edge(clk) then
			if io_wr='1' then
				if io_addr='0' then
					clk_divider <= io_din;
				end if;
			end if;
		end if;
	end process;
	
	rx_rd <= io_rd and io_addr;
	tx_wr <= io_wr and io_addr;
	
	process (io_addr,rx_dout,tx_ready,rx_ready)
	begin
		if io_addr='1' then
			io_dout(7 downto 0)	<=	rx_dout;
		else
			io_dout(7 downto 2)	<= (others=>'0');
			io_dout(1)				<= tx_ready;
			io_dout(0)				<= rx_ready;
		end if;
	end process;
	io_dout(15 downto 8)	<=	(others=>'0');
			
end Behavioral;

