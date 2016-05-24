module lang::css::m3::examples::RulesetDuplication

import lang::css::m3::AST;
import lang::css::m3::Core;
import IO;
import List;
import Tuple;
import String;
import Relation;
import Prelude;
import util::Math;
import demo::common::Crawl;

M3 stylesheetM3 = createM3FromFile(|home:///workspace/testCSS/sandbox/ruleclones.css|);
Statement stylesheetAST = createAstFromFile(|home:///workspace/testCSS/sandbox/ruleclones.css|);

public void detectRulesetDuplication() {

	list[Type] currentSelectors = [];
	list[Declaration] currentDeclarations = [];
	
	rel[Type, list[Declaration]] allRules = {}; 

	visit (stylesheetAST) {
		case ruleSet(list[Type] selector, list[Declaration] declarations): {
			if (size(currentSelectors) > 0 && size(currentDeclarations) > 0) {
				for (sel <- currentSelectors) {
					if (<sel, sort(currentDeclarations)> in allRules) {
						iprintln("Clone alert");
					} else {
						allRules += <sel, sort(currentDeclarations)>;
					}
				}
			}
			currentSelectors = [];
			currentDeclarations = [];
		}
		case combinedSelector(list[Expression] selectors): {
			currentSelectors += combinedSelector(selectors);
		}
		case declaration(str property, list[Type] values): {
			currentDeclarations += declaration(property, values);
		}
	};
}