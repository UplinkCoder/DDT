▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ basic cases 
mixin("int foo");
mixin(123);
#STRUCTURE_EXPECTED:
DeclarationMixinString(ExpLiteralString)
DeclarationMixinString(ExpLiteralInteger)

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ (duplicate test case, here for clarity)
#PARSE(DECLARATION)   mixin #@NO_TYPEREF #error(EXP_SEMICOLON) #@BREAK
#STRUCTURE_EXPECTED:  DeclarationMixin(#@NO_TYPEREF)

▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin ( #@EXP_ANY__LITE #@CLOSEPARENS_OR_NO #@SEMICOLON_OR_BREAK
#STRUCTURE_EXPECTED:  DeclarationMixinString(#@EXP_ANY__LITE)
▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂▂ 
#PARSE(DECLARATION)   mixin(#@EXP_ANY);
#STRUCTURE_EXPECTED:  DeclarationMixinString(#@EXP_ANY)
