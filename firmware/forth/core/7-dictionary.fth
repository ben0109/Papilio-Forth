( dictionary )

: __def_get_head		$dictHead @ ;
: __def_get_previous	@ ;
: __def_get_flags		2+ c@ 0xe0 and ;
: __def_get_xt			2+ dup c@ 0x1f and swap 1+ + 1+ 2/ 2* ;

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

: cfind
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

: find					count cfind ;
