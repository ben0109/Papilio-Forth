LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

ENTITY uart_tx_tb IS
END uart_tx_tb;
 
ARCHITECTURE behavior OF uart_tx_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT uart_tx
    PORT(
         clk : IN  std_logic;
         clk_divider : in std_logic_vector(15 downto 0);
         d_in : IN  std_logic_vector(7 downto 0);
         write : IN  std_logic;
         ready : OUT  std_logic;
         serial_out : OUT  std_logic
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal clk_divider : std_logic_vector(15 downto 0);
   signal d_in : std_logic_vector(7 downto 0) := (others => '0');
   signal write : std_logic := '0';

 	--Outputs
   signal ready : std_logic;
   signal serial_out : std_logic;

   -- Clock period definitions
   constant clk_period : time := 31.25 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: uart_tx PORT MAP (
          clk => clk,
          clk_divider => clk_divider,
          d_in => d_in,
          write => write,
          ready => ready,
          serial_out => serial_out
        );

   -- Clock process definitions
   clk_process :process
   begin
		clk <= '0';
		wait for clk_period/2;
		clk <= '1';
		wait for clk_period/2;
   end process;
 

   -- Stimulus process
   stim_proc: process
   begin
		clk_divider <= "0000110100000100"; -- 3332 for 9600 bauds @ 32MHz
		write <= '0';
      wait for clk_period*1000;

		d_in <= "01111001";
		write <= '1';
      wait for clk_period;

		write <= '0';
      wait;
   end process;

END;
