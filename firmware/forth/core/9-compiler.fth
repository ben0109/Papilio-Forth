(
:c environment? environment-query unimplemented ;
: evaluate	unimplemented ;
)
: execute	{ ld pc,ds } ;


: literal
	$ld_ds_next , ,
;
: >body 2+ @ ;

: recurse			$lastDef @ __def_get_xt , ; immediate
(
: postpone			unimplemented ; immediate
: s" s-quote		unimplemented ;
)

: state				$state @ ;
: immediate			$lastDef @ 2+ dup c@ 0x80 or swap c! ;
: '					0x20 word find drop ;
: __parse_and_exec
		( read some data )
		begin __at-end while
			cr ." ?>"
			$inputBuffer 80 accept $inputSize ! 0 >in !
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
				state if literal then
			else
				2drop 
				." unknown word "
				count type 
			then
		else
			rot drop			( drop the word pointer )
			0 < state and if
				,
			else
				execute
			then
		then
;
: __main
	0xd04 0xfffc !
	12 emit
	." Welcome to Papilio Forth" cr 
	begin
		__parse_and_exec
	repeat
;

: __do_colon
	align
	here $lastDef !
	$dictHead @ ,
	over c@ or c,
	dup 1+ swap c@ 0 do dup c@ c, 1+ loop drop
	align
;
: __make_def_visible $lastDef @ $dictHead ! ;
: __do_semicolon
	$ld_pc_rs ,
	__make_def_visible
;
: xt-does
{ xt-does: }
	$lastDef @					( load previous def address )
	dup 1+						( create new def w/ same name )
	over c@
	__do_colon
	__def_get_xt ,				( start by calling previous def )
	$jmp_al ,					( jump al,... )
	r> ,						( ...after call of xt-does )
	__make_def_visible			( add def to dictionary )
;								( return to caller of caller )
: does> 						{ ld ds, xt-does } , ; immediate

: create
	bl word						( get name )
	0 __do_colon				( begin def )
	here literal					( load address )
	__do_semicolon				( return )
;
: variable						create 1 cells allot ;
: constant						create , does> @ ;

: [ 							false $state ! ; immediate
: ] 							true $state ! ;

: [char] 						postpone [ char ] ;
: ['] 							postpone [ ' ] literal ;

: ; 							__do_semicolon postpone [ ; immediate
: : 							bl word 0 __do_colon ] ;

