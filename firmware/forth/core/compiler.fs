
: recurse       $lastDef literal @ __def_get_xt , ; immediate
: immediate     $lastDef literal @ 2+ dup c@ 0x80 or swap c! ;
: '             0x20 word find drop ;
: [             false $state literal ! ; immediate
: ]             true $state literal ! ;

: [char]        postpone [ char ] ; immediate
: [']           postpone [ ' ] postpone literal ; immediate
: postpone      postpone [ ' ] scall ; immediate



: __do_colon    ( c-addr n1 n2 -- )
    align
    here $lastDef literal !
    $dictHead literal @ ,
    over or c,
    0 do dup c@ c, 1+ loop drop
    align
;
: __make_def_visible
    $lastDef literal @ $dictHead literal !
;
: __do_semicolon
    0x700c ,                          ( return )
    __make_def_visible
;
: does>
    $lastDef literal @                ( load previous def address )
    dup __def_get_name 0 __do_colon   ( create new def w/ same name )
    __def_get_xt scall                ( start by calling previous def )
    r> ubranch                        ( jump to after call of xt-does )
    __make_def_visible                ( add def to dictionary )
;                                     ( return to caller of caller )
: create
    bl word count                     ( get name )
    0 __do_colon                      ( begin def )
    here 4 + postpone literal         ( load address -- to be patched )
    __do_semicolon                    ( return )
;
: >body         execute ;
: variable      create 1 cells allot ;
: constant      create , does> @ ;

: ;             __do_semicolon postpone [ ; immediate
: :             bl word count 0 __do_colon ] ;

