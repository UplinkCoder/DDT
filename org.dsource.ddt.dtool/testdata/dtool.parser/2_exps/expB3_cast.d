▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION) cast(Foo) foo
#AST_STRUCTURE_EXPECTED:
ExpCast(RefIdentifier #@ExpIdentifier)
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION) cast() foo
#AST_STRUCTURE_EXPECTED:
ExpCast( #@MISSING_REF #@ExpIdentifier )
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION) cast(const shared) foo
#AST_STRUCTURE_EXPECTED:
ExpCastQual( #@ExpIdentifier )

Ⓗ▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#@UNEXP_OR_NO《#@EXP_UNARY__NO_REFS●#@NO_EXP》

#@CAST_QUAL1《const●inout●immutable●shared》
#@CAST_QUAL2《const shared●shared const●inout shared●shared inout》
#@CAST_START《
  ►#?AST_STRUCTURE_EXPECTED!【cast( #@TYPE_REFS )● ExpCast( #@TYPE_REFS 】● 
  ►#?AST_STRUCTURE_EXPECTED!【cast( )●             ExpCast( #@MISSING_REF 】● 
  ►#?AST_STRUCTURE_EXPECTED!【cast( #@CAST_QUAL1 )● ExpCastQual(】●
  ►#?AST_STRUCTURE_EXPECTED!【cast( #@CAST_QUAL2 )● ExpCastQual(】●
¤》

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       #@CAST_START  #@UNEXP_OR_NO
#AST_STRUCTURE_EXPECTED: #@CAST_START  #@UNEXP_OR_NO )
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       cast   ( #@TYPE_REFS #error(EXP_CLOSE_PARENS) #parser(IgnoreRest) foo  
#AST_STRUCTURE_EXPECTED: ExpCast( #@TYPE_REFS )
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       cast   ( #@CAST_QUAL1 #error(EXP_OPEN_PARENS) #error(EXP_CLOSE_PARENS) #parser(IgnoreRest) foo  
#AST_STRUCTURE_EXPECTED: ExpCast( RefTypeModifier() )
#AST_SOURCE_EXPECTED:    cast   ( #@CAST_QUAL1 )
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       cast   ( #@CAST_QUAL2 #error(EXP_CLOSE_PARENS) #parser(IgnoreRest) foo  
#AST_STRUCTURE_EXPECTED: ExpCastQual( )

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       cast   (  #error(EXP_CLOSE_PARENS) / 7  
#AST_STRUCTURE_EXPECTED: ExpInfix(ExpCast( #@MISSING_REF ) Integer) 
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂
#PARSE(EXPRESSION)       cast   #error(EXP_OPEN_PARENS) / 7
#AST_STRUCTURE_EXPECTED: ExpInfix(ExpCast( )               Integer) 
#AST_SOURCE_EXPECTED:    cast / 7