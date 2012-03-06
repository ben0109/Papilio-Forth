( control words )

: if
	$ldf_a_ds ,			( ldf a,ds )
	$jmp_eq ,			( jmp eq,... )	
	here				( put marker on stack )
	0 ,					( space for address )
; immediate

: then
	here swap !			( patch jump for 'if' )
; immediate

: else
	$jmp_al ,			( jmp al,... )
	here				( put marker on stack )
	0 ,					( space for address )
	swap postpone then	( patch 'if' jump )
; immediate

: exit
	$ld_pc_rs ,	( ld pc,rs )
; immediate





: TOK_START			0 ;
: TOK_LEAVE			1 ;

: __finish_loop
	here 2+							( address after loop address )
	{ ld	b,ds }
	{ finish-loop-start: }
	{ ldf	a,ds }					( test marker )
	{ jmp	eq,finish-loop-end }	( 0 => start token )
	{ ld	a,ds }					( load address )
	{ ld	(a),b }					( patch jump )
	{ jmp	al,finish-loop-start }
	{ finish-loop-end: }
	,								( tos is loop start address )
;

: begin
	here TOK_START					( put begin token on stack )
; immediate

: while
	$ldf_a_ds ,						( "ldf a,ds" )
	$jmp_eq ,						( "jmp eq,..." )
	here TOK_LEAVE					( put leave token on stack )
	0 ,								( reserve space )
; immediate

: until
	$ldf_a_ds ,						( "ldf a,ds" )
	$jmp_eq ,						( "jmp ne,..." )
	__finish_loop
; immediate

: repeat
	$jmp_al ,						( "jmp al,..." )
	__finish_loop
; immediate


{ xt-do: }
{ 	ld	a,rs }
{ 	ld	rs,ds }
{ 	ld	rs,ds }
{ 	ld	pc,a }

{ xt-loop: }
{ 	ld	ds,rs }
{ 	ld	ds,rs }
{ 	add	rs,#1 }
{ 	ld	a,rs }
{ 	ld	rs,a }
{ 	ld	rs,ds }
{ 	ld	ds,a }
{ 	ld	a,rs }
{ 	ld	rs,a }
{ 	sub	a,ds }
{ 	jmp	eq,xt-loop-quit }
{ 	ld	a,ds }
{ 	ld	pc,(a) }
{ xt-loop-quit: }
{ 	ld	a,rs }
{ 	ld	a,rs }
{ 	add	ds,#2 }
{ 	ld	pc,ds }

: do
	{ ld	ds,xt-do } ,			( call xt-do )
	here TOK_START					( put do token on stack )
; immediate

: loop
	{ ld	ds,xt-loop } , 			( call xt-loop )
	__finish_loop
; immediate

: unloop
	r>								( save return address )
	r> drop							( drop counter )
	r> drop							( drop limit )
	>r								( restore return address )
;

: leave
	postpone unloop					( call unloop )
	$jmp_al ,						( "jump al,..." )
	here TOK_LEAVE					( put leave token on stack ) 
	0 ,								( reserve space )
; immediate

: i r@ ;
: j r> r> r@ >r >r ;


	