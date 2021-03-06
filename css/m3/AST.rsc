module lang::css::m3::AST

extend analysis::m3::AST;

import util::FileSystem;
import lang::css::m3::TypeSymbol;
import IO;
import Set;
import String;
import List;
import util::Math;

anno str Declaration@documentation;

anno str Statement@documentation;
anno str Statement@modifier;

anno str Expression@combinator;

anno str Type@operator;
anno loc Type@src;

data Declaration
	// Rulesets
    = stylesheet(str name, list[Declaration] rules) // #lol { color: red; } #lal { color: blue; }
    | ruleSet(list[Expression] selector, list[Statement] declarations) // #lol { color: red; } (selector is a list because "#lol, #hi, #hej" are 3 individual selectors) (@TODO selector is a list of types because of the combinedSelector)
    // At rules
    | ruleMedia(list[Type] mediaQueries, list[Declaration] ruleSets) // @media only screen and (max-width : 480px) {
    | ruleFontFace(list[Statement] declarations) // @font-face
    | ruleImport(str uri) // @import url("style2.css");
    | ruleImport(str uri, list[Type] mediaQueries) // @import url("style2.css") handheld and (max-width: 400px);
    | ruleCounterStyle(str name, list[Statement] declarations)
    | ruleNameSpace(str prefix, str uri)
    | ruleNameSpace(str uri)
    | ruleCharset(str name)
    | ruleKeyframes(str name, list[Declaration] ruleSets)
    | rulePage(str pseudo, list[Statement] declarations) // @page :left { "delcarations here" }
    | ruleViewport(list[Statement] declarations) // @viewport { "delcarations here" }
    ;

data Statement
    = declaration(str property, list[Type] values) // color: red;, background: url('logo.png') no-repeat;
    ;

data Expression 
	= combinedSelector(list[Expression] selectors) // .lol > .hej div.you a single complete selector
    | selector(list[Type] simpleSelector) // .lol.hej or #hej
    | mediaExpression(str property, list[Type] values) // (max-width : 480px) and (orientation: landscape) in mediaqueries like "@media tv and (min-width: 700px) and (orientation: landscape)"
    ;

data Type 
	// Selector
    = class(str name) // .lol
    | id(str name) // #lol
    | domElement(str name) // div
    | attributeSelector(str attribute, str op, str attrval) // div[class*="post"] (This rule only relates to the [class*="post"] part)
    | attributeSelector(str attribute) // div[disabled] (This rule only relates to the [disabled] part)
    | pseudoClass(str class) // :after, :link, :first-child
    | pseudoElement(str elem) // ::first-letter, ::after
    // Values
    | audio(num aud, str unit) // 10db, -10db
    | angle(num angle, str unit) // 10deg, 10grad, 1rad, 0.25turn
    | color(int red, int green, int blue, num alpha) // red, #000000, #888, rgb(0,0,255), rgb(0,0,255,0.5), hsl(120, 100%, 50%), hsla(120, 100%, 50%, 0.3)
    | expression(str expression) // top: expression(body.scrollTop + 50 + "px");
    | calc(str expression) // top: calc(100% - 50px);
    | frequency(num freq, str unit) // 12Hz, 14KhZ (No space between the number and the literal! Not supported by any browser so far)
    | function(str func, list[Type] exp) // url(lol.jpg), calc(100% - 100px), linear-gradient(red,yellow,blue). Example for content property: 'content: " (" attr(href) ")";'
    | ident(str ident) // left, auto, none
    | integer(int integer) // 1
    | length(num len, str unit) // 10cm, 10.00pt, 10em, etc.
    | percent(num perc) // 10%, 10.00%
    | number(num number) // 10.00
    | resolution(num res, str unit) // 960dpi, 10dpcm, 20dppx
    | string(str string) // "lol"
    | time(num time, str unit) // 12s, +0s, -456ms
    | uri(str uri) // url("paper.jpg"), url(paper.jpg), url("http://nicasso.nl/assets/css/images/logo.png")
    // Exotics
    | mediaQuery(str \type, list[Expression] expressions) // @media tv and (min-width: 700px) and (orientation: landscape) where "tv" is the type and "orientation: landscape" is a mediaExpression
    ;

data Modifier
    = important() // !important
    | combinator(str combi) // Combinator for selectors such as > + and ~
    | operator(str op) // Operator between values such as a comma
    ;
    
public Declaration createAstFromFile(loc file) {
    result = createAstsFromFiles({file});
    if ({oneResult} := result) {
        return oneResult;
    }
    throw "Unexpected number of ASTs returned from <file>";
}

public set[Declaration] createAstsFromDirectory(loc project) {
    if (!(isDirectory(project))) {
      throw "<project> is not a valid directory";
    }
    
    allCSSFiles = { j | j <- find(project, "css"), isFile(j) };
    return createAstsFromFiles(allCSSFiles);
}

@javaClass{org.rascalmpl.library.lang.css.m3.internal.ast.ASTLoader}
@reflect{Need access to stderr and stdout}
public java set[Declaration] createAstsFromFiles(set[loc] file);