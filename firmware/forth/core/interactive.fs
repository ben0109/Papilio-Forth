( dictionary )

: __def_get_head		$dictHead literal @ ;
: __def_get_previous	@ ;
: __def_get_flags		2+ c@ 0xe0 and ;
: __def_get_name		2+ dup 1+ swap c@ 0x1f and ;
: __def_get_xt			2+ dup c@ 0x1f and swap 1+ + 1+ 2/ ;

( compare two strings )
( c-addr1 n1 c-addr2 n2 -- flag )
: s=
	>r swap r@ = if
		true r> 0 do
			>r
			2dup c@ swap c@ - 0= r> and >r
			1+ swap 1+
			r>
		loop
		>r 2drop r>
	else
		r> drop 2drop false
	then
;

: find
	count
	__def_get_head >r
	begin
		2dup r@ 2+ dup 1+ swap c@ 0x1f and 
		s= if
			2drop
			r@ __def_get_xt
			r> __def_get_flags 0x80 and if 1 else -1 then
			exit
		then
		r> __def_get_previous >r
	r@ 0= until
	r> drop 2drop 0
;









( input and parsing )

( read a char from UART )
: __getc				begin 0x4000 @ 1 and until 0x4001 @ ;

: >in					$toIn literal ;
: source				$inputBuffer literal $inputSize literal @ ;
: __at-end				$inputSize literal @ >in @ = ;
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
	$inputBuffer literal >in @ + c@ 
	>in 1+!
;
: word
	>r
	begin 
		__at-end if
			r> drop
			$wordBuffer literal dup 0 c!
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
	$wordBuffer literal 1+ 
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
	$wordBuffer literal 
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







(
:c environment? environment-query unimplemented ;
: evaluate      unimplemented ;
)
: execute		2* >r exit ;
: state         $state literal @ ;

: xt-s"			r> count 2dup + aligned >r ;
: s"
	[ ' xt-s" ] literal scall
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
	[ ' type ] literal scall
; immediate


: depth		dsp 0x1f and ;

: __parse_and_exec
    ( read some data )
    begin __at-end while
        cr ." ?>"
        $inputBuffer literal 80 accept $inputSize literal ! 0 >in !
    repeat

    ( parse a word )
    bl word
    
    ( empty => do nothing )
    dup c@ 0= if
        drop exit
    then

    ( not empty => lookup dictionary )
    dup find

    ( not found => try number )
    dup 0= if
        drop
        0 over count >number
        0= if
            rot 2drop
            state if postpone literal then
        else
            ." unknown word "
            2drop 
            count type 
        then
    else
        rot drop            ( drop the word pointer )
        0 < state and if
            scall
        else
            execute
        then
    then
;
: __main
	0x1a08 0x4000 ! 12 emit
    ." Welcome to Papilio Forth" cr
    begin
        __parse_and_exec
		cr
		depth dup >r
		begin
			dup while
			swap
			dup __print_word
			r> swap >r >r
			space 1-
		repeat
		drop cr r>
		begin
			dup while
			r> swap
			1-
		repeat
		drop
    repeat
;



