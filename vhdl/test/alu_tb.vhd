LIBRARY ieee;
USE ieee.std_logic_1164.ALL;
 
ENTITY alu_tb IS
END alu_tb;
 
ARCHITECTURE behavior OF alu_tb IS 
 
    -- Component Declaration for the Unit Under Test (UUT)
 
    COMPONENT alu
    PORT(
         alu_op : IN  std_logic_vector(3 downto 0);
         alu_in1 : IN  std_logic_vector(15 downto 0);
         alu_in2 : IN  std_logic_vector(15 downto 0);
         alu_out : OUT  std_logic_vector(15 downto 0);
         flag_c : IN  std_logic;
         flag_v : IN  std_logic;
         flag_z : IN  std_logic;
         flag_s : IN  std_logic;
         new_c : OUT  std_logic;
         new_v : OUT  std_logic;
         new_z : OUT  std_logic;
         new_s : OUT  std_logic
        );
    END COMPONENT;
    

   --Inputs
   signal alu_op : std_logic_vector(3 downto 0) := (others => '0');
   signal alu_in1 : std_logic_vector(15 downto 0) := (others => '0');
   signal alu_in2 : std_logic_vector(15 downto 0) := (others => '0');
   signal flag_c : std_logic := '0';
   signal flag_v : std_logic := '0';
   signal flag_z : std_logic := '0';
   signal flag_s : std_logic := '0';

 	--Outputs
   signal alu_out : std_logic_vector(15 downto 0);
   signal new_c : std_logic;
   signal new_v : std_logic;
   signal new_z : std_logic;
   signal new_s : std_logic;
 
BEGIN
 
	-- Instantiate the Unit Under Test (UUT)
   uut: alu PORT MAP (
          alu_op => alu_op,
          alu_in1 => alu_in1,
          alu_in2 => alu_in2,
          alu_out => alu_out,
          flag_c => flag_c,
          flag_v => flag_v,
          flag_z => flag_z,
          flag_s => flag_s,
          new_c => new_c,
          new_v => new_v,
          new_z => new_z,
          new_s => new_s
        );

   -- Stimulus process
   stim_proc: process
   begin
		alu_op <= "1101";
		alu_in1 <= "0111111111111111";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		flag_c <= '1';
      wait for 10 ns;
		alu_in1 <= "1111111111111111";
		alu_in2 <= "0000000000000000";
		flag_c <= '0';
      wait for 10 ns;
		flag_c <= '1';
      wait for 10 ns;
		alu_in1 <= "1111111111111111";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		flag_c <= '1';
      wait for 10 ns;
		
		alu_op <= "1111";
		alu_in1 <= "0000000000000000";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		alu_in1 <= "0000000000000001";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		alu_in1 <= "1000000000000000";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		alu_in1 <= "1000000000000001";
		alu_in2 <= "0000000000000001";
		flag_c <= '0';
      wait for 10 ns;
		alu_in1 <= "1000000000000001";
		alu_in2 <= "1000000000000000";
		flag_c <= '0';
      wait for 10 ns;
      wait;
   end process;

END;
