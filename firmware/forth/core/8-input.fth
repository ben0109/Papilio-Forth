( input and parsing )

: >in					$toIn ;
: >in					$toIn ;
: source				$inputBuffer $inputSize @ ;
: __at-end				$inputSize @ >in @ = ;
: __getc				begin 0xfffc @ 1 and until 0xfffe @ ;
: key
	begin
		__getc
		dup dup 0x1f > swap 0x7f < and if exit then
		dup 8 = if exit then
		dup 10 = if exit then
		dup 13 = if drop 10 exit then
		27 = if __getc then
	repeat
;
: accept
	>r 0 >r
	begin
		key
		dup emit
		dup 10 = if
			drop true
		else 
			dup 8 = if
				drop
				r@ if r> 1- >r then
			else
				over r@ + c!
				r> 1+ >r
			then
			false
		then
		r> r@ over >r = or
	until
	drop r> r> drop
;
: __next-char
	$inputBuffer >in @ + c@ 
	>in 1+!
;
: word
	>r
	begin 
		__at-end if
			r> drop
			$wordBuffer dup 0 c!
			exit
		else
			__next-char
			dup r@ = if
				drop false
			else
				true
			then
		then
	until
	$wordBuffer 1+ 
	swap over c! 1+
	begin
		__at-end if
			true
		else
			__next-char
			dup r@ = if
				drop true
			else
				over c! 1+ false
			then
		then
	until
	r> drop
	$wordBuffer  
	>r r@ 1+ - r@ c! r>
;
: char		bl word 1+ c@ ;
: >number
	>r
	begin r@ while
		dup c@
		dup dup 0x2f > swap 0x3a < and if
			0x30 -
		else
			dup dup 0x60 > swap 0x57 base + < and if
				0x57 -
			else
				drop r> exit
			then 
		then
		rot base * +
		swap 1+
		r> 1- >r
	repeat
	r>
;

: xt-s"
{ xt-s-quote: }
	r> count 2dup + aligned >r
;

: s"
	{ ld ds,xt-s-quote } ,
	here 0
	0 c,
	begin
		__next-char
		dup 0x22 - if
			c, 1+ false
		else
			drop true
		then
	until
	align
	swap c!
; immediate
	
: ."
	postpone s"
	{ ld ds, xt-type } ,
; immediate


