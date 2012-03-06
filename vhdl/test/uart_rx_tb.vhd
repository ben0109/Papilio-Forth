--------------------------------------------------------------------------------
-- Company: 
-- Engineer:
--
-- Create Date:   21:41:15 03/03/2012
-- Design Name:   
-- Module Name:   /home/ben/prog/PapilioForth/ise/uart_rx_tb.vhd
-- Project Name:  PapilioForth
-- Target Device:  
-- Tool versions:  
-- Description:   
-- 
-- VHDL Test Bench Created by ISE for module: uart_rx
-- 
-- Dependencies:
-- 
-- Revision:
-- Revision 0.01 - File Created
-- Additional Comments:
--
-- Notes: 
-- This testbench has been automatically generated using types std_logic and
-- std_logic_vector for the ports of the unit under test.  Xilinx recommends
-- that these types always be used for the top-level I/O of a design in order
-- to guarantee that the testbench will bind correctly to the post-implementation 
-- simulation model.
--------------------------------------------------------------------------------
LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
use IEEE.NUMERIC_STD.ALL;
 
-- Uncomment the following library declaration if using
-- arithmetic functions with Signed or Unsigned values
--USE ieee.numeric_std.ALL;
 
ENTITY uart_rx_tb IS
END uart_rx_tb;
 
ARCHITECTURE behavior OF uart_rx_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT uart_rx
    PORT(
         clk : IN  std_logic;
         clk_divider : IN  std_logic_vector(15 downto 0);
         serial_in : IN  std_logic;
         read : IN  std_logic;
         ready : OUT  std_logic;
         d_out : OUT  std_logic_vector(7 downto 0)
        );
    END COMPONENT;
    

   --Inputs
   signal clk : std_logic := '0';
   signal clk_divider : std_logic_vector(15 downto 0) := (others=>'0');
   signal serial_in : std_logic := '0';
   signal read : std_logic := '0';

 	--Outputs
   signal ready : std_logic;
   signal d_out : std_logic_vector(7 downto 0);
   signal counter : unsigned(3 downto 0);

   -- Clock period definitions
   constant clk_period : time := 31.25 ns;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: uart_rx PORT MAP (
          clk => clk,
          clk_divider => clk_divider,
          serial_in => serial_in,
          read => read,
          ready => ready,
          d_out => d_out
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
		serial_in <= '1';
      wait for 325us;

		serial_in <= '0';
		wait for 104us;
		serial_in <= '1';
		wait for 104us;
		serial_in <= '0';
		wait for 104us;
		serial_in <= '0';
		wait for 104us;
		serial_in <= '1';
		wait for 104us;
		serial_in <= '0';
		wait for 104us;
		serial_in <= '1';
		wait for 104us;
		serial_in <= '1';
		wait for 104us;
		serial_in <= '0';
		wait for 104us;
		serial_in <= '1';

      wait;
   end process;

END;
