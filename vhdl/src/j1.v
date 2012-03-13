/*
Copyright (c)  2011
    James Bowman  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
3. Neither the name of James Bowman nor the names of its contributors
  may be used to endorse or promote products derived from this software
  without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
*/

module j1(
   input sys_clk_i, input sys_rst_i, input [15:0] io_din,
   output io_rd, output io_wr, output [15:0] io_addr, output [15:0] io_dout);

  wire [15:0] insn;
  wire [15:0] immediate = { 1'b0, insn[14:0] };

  wire [15:0] ramrd;

  reg [4:0] dsp;  // Data stack pointer
  reg [4:0] _dsp;
  reg [15:0] st0; // Return stack pointer
  reg [15:0] _st0;
  wire _dstkW;     // D stack write

  reg [12:0] pc;
  reg [12:0] _pc;
  reg [4:0] rsp;
  reg [4:0] _rsp;
  reg _rstkW;     // R stack write
  reg [15:0] _rstkD;
  wire _ramWE;     // RAM write enable

  wire [15:0] pc_plus_1;
  assign pc_plus_1 = pc + 1;

  // The D and R stacks
  reg [15:0] dstack[0:31];
  reg [15:0] rstack[0:31];
  always @(posedge sys_clk_i)
  begin
    if (_dstkW)
      dstack[_dsp] = st0;
    if (_rstkW)
      rstack[_rsp] = _rstkD;
  end
  wire [15:0] st1 = dstack[dsp];
  wire [15:0] rst0 = rstack[rsp];

  // st0sel is the ALU operation.  For branch and call the operation
  // is T, for 0branch it is N.  For ALU ops it is loaded from the instruction
  // field.
  reg [3:0] st0sel;
  always @*
  begin
    case (insn[14:13])
      2'b00: st0sel = 0;          // ubranch
      2'b10: st0sel = 0;          // call
      2'b01: st0sel = 1;          // 0branch
      2'b11: st0sel = insn[11:8]; // ALU
      default: st0sel = 4'bxxxx;
    endcase
  end

`define RAMS 3

  genvar i;

`define w (16 >> `RAMS)
`define w1 (`w - 1)

  generate 
    for (i = 0; i < (1 << `RAMS); i=i+1) begin : ram
      // RAMB16_S18_S18
      RAMB16_S2_S2
      ram(
        .DIA(0),
        // .DIPA(0),
        .DOA(insn[`w*i+`w1:`w*i]),
        .WEA(0),
        .ENA(1),
        .CLKA(sys_clk_i),
        .ADDRA({_pc}),

        .DIB(st1[`w*i+`w1:`w*i]),
        // .DIPB(2'b0),
        .WEB(_ramWE & (_st0[15:14] == 0)),
        .ENB(|_st0[15:14] == 0),
        .CLKB(sys_clk_i),
        .ADDRB(_st0[15:1]),
        .DOB(ramrd[`w*i+`w1:`w*i]));
    end
  endgenerate
/*
   RAMB16_S18_S18 #(
      .INIT_00(256'h6820_5B20_303B_0000_00C6_00C6_0000_00F0_0000_0000_0000_0000_0000_0000_0000_0068),
      .INIT_01(256'h0000_0000_0000_0000_5D20_2120_2D31_202D_3120_6572_6568_202D_3120_2F32_2065_7265),
      .INIT_02(256'h0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000),
      .INIT_03(256'h0000_0000_0000_0000_0000_0000_0000_0000_0000_0001_0001_0000_0000_0000_0000_0000),
      .INIT_04(256'h0096_710F_6023_2101_008E_708D_7075_6403_0000_0000_0000_0000_0000_0000_0000_0000),
      .INIT_05(256'h0065_7265_6804_00AE_7D0F_8001_002F_3202_00A6_7A0C_002D_3102_009E_740F_0072_6F02),
      .INIT_06(256'h6303_8001_6C00_C000_6103_6023_C000_9A08_006E_6961_6D5F_5F06_00B8_700C_6C00_8010),
      .INIT_07(256'h0000_0000_0000_0000_0000_0000_0000_0000_700C_0076_6103_6023_C001_6C00_C001_206C)
   ) ram (
        .DIA(0),
        .DIPA(2'b0),
        .DOA(insn[15:0]),
        .WEA(0),
        .ENA(1),
        .CLKA(sys_clk_i),
        .ADDRA({_pc[9:0]}),

        .DIB(st1[15:0]),
        .DIPB(2'b0),
        .WEB(_ramWE & (_st0[15:14] == 0)),
        .ENB(|_st0[15:14] == 0),
        .CLKB(sys_clk_i),
        .ADDRB(_st0[10:1]),
        .DOB(ramrd[15:0]));
*/
  // Compute the new value of T.
  always @*
  begin
    if (insn[15])
      _st0 = immediate;
    else
      case (st0sel)
        4'b0000: _st0 = st0;
        4'b0001: _st0 = st1;
        4'b0010: _st0 = st0 + st1;
        4'b0011: _st0 = st0 & st1;
        4'b0100: _st0 = st0 | st1;
        4'b0101: _st0 = st0 ^ st1;
        4'b0110: _st0 = ~st0;
        4'b0111: _st0 = {16{(st1 == st0)}};
        4'b1000: _st0 = {16{($signed(st1) < $signed(st0))}};
        4'b1001: _st0 = st1 >> st0[3:0];
        4'b1010: _st0 = st0 - 1;
        4'b1011: _st0 = rst0;
        4'b1100: _st0 = |st0[15:14] ? io_din : ramrd;
        4'b1101: _st0 = st1 << st0[3:0];
        4'b1110: _st0 = {rsp, 3'b000, dsp};
        4'b1111: _st0 = {16{(st1 < st0)}};
        default: _st0 = 16'hxxxx;
      endcase
  end

  wire is_alu = (insn[15:13] == 3'b011);
  wire is_lit = (insn[15]);

  assign io_rd = (is_alu & (insn[11:8] == 4'hc));
  assign io_wr = _ramWE;
  assign io_addr = st0;
  assign io_dout = st1;

  assign _ramWE = is_alu & insn[5];
  assign _dstkW = is_lit | (is_alu & insn[7]);

  wire [1:0] dd = insn[1:0];  // D stack delta
  wire [1:0] rd = insn[3:2];  // R stack delta

  always @*
  begin
    if (is_lit) begin                       // literal
      _dsp = dsp + 1;
      _rsp = rsp;
      _rstkW = 0;
      _rstkD = _pc;
    end else if (is_alu) begin
      _dsp = dsp + {dd[1], dd[1], dd[1], dd};
      _rsp = rsp + {rd[1], rd[1], rd[1], rd};
      _rstkW = insn[6];
      _rstkD = st0;
    end else begin                          // jump/call
      // predicated jump is like DROP
      if (insn[15:13] == 3'b001) begin
        _dsp = dsp - 1;
      end else begin
        _dsp = dsp;
      end
      if (insn[15:13] == 3'b010) begin // call
        _rsp = rsp + 1;
        _rstkW = 1;
        _rstkD = {pc_plus_1[14:0], 1'b0};
      end else begin
        _rsp = rsp;
        _rstkW = 0;
        _rstkD = _pc;
      end
    end
  end

  always @*
  begin
    if (sys_rst_i)
      _pc = pc;
    else
      if ((insn[15:13] == 3'b000) |
          ((insn[15:13] == 3'b001) & (|st0 == 0)) |
          (insn[15:13] == 3'b010))
        _pc = insn[12:0];
      else if (is_alu & insn[12])
        _pc = rst0[15:1];
      else
        _pc = pc_plus_1;
  end

  always @(posedge sys_clk_i)
  begin
    if (sys_rst_i) begin
      pc <= 0;
      dsp <= 0;
      st0 <= 0;
      rsp <= 0;
    end else begin
      dsp <= _dsp;
      pc <= _pc;
      st0 <= _st0;
      rsp <= _rsp;
    end
  end

endmodule // j1
