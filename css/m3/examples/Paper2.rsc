module lang::css::m3::examples::Paper2

import lang::css::m3::AST;
import lang::css::m3::Core;
import lang::css::m3::PrettyPrinter;

import IO;
import String;
import Relation;
import Set;
import Map;
import Node;
import List;
import util::Math;
import DateTime;

/**
 * From the paper: Complexity Metrics for Cascading Style Sheets
 */
 
Statement stylesheetAST; 
 
public void metrics1() {

	list[loc] dirs = [
		|home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/360.cn.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/alieexpress.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/amazon.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/apple.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/baidu.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/bing.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/blogger.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/chinadaily.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/diply.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/ebay.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/facebook.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/fc2.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/gmw.cn.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/google.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/hao123.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/imdb.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/imgur.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/instagram.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/kat.cr.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/linkedin.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/live.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/mail.ru.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/microsoft.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/msn.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/naver.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/netflix.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/office.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/ok.ru.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/onclickads.net.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/paypal.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/pinterest.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/pornhub.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/qq.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/reddit.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/sina.com.cn.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/sohu.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/stackoverflow.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/taobao.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/tmall.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/tumblr.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/twitter.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/vk.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/weibo.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/whatsapp.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/wikipedia.org.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/wordpress.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/xvideos.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/yahoo.com.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/yandex.ru.css|,
	    |home:///Documents/workspace/Rascal/rascal/testCSS/sample-set/web-new/youtube.com.css|
	];
	
	for (d <- dirs) {
		//datetime begin = now();
		stylesheetAST = createAstFromFile(d);
		//datetime end = now();
		
		//diff = end-begin;
		//iprintln("<d.file> <diff>");
		
		//iprintln("<d> & <ruleLength()> & <numberOfRuleBlocks()> & <numberOfAttributesDefinedPerRuleBlock()> & <numberOfCohesiveRuleBlocks()>");
		
		println(precision(numberOfAttributesDefinedPerRuleBlock(),3)); 
	}
}

public int ruleLength() {
	int ruleLength = 0;
	list[loc] hi = [rs@src | /rs:ruleSet(list[Type] selector, list[Declaration] declarations) := stylesheetAST];
	for (h <- hi) {
		ruleLength += ruleLengthHelper(h);
	}
	return ruleLength;
}
 
public int ruleLengthHelper(loc rule) {	
	int ruleLength = 0;
	bool commentBlock = false;
	for (line <- readFileLines(rule)) {	
		if (contains(line,"/*") && !contains(line,"*/")) {
			commentBlock = true;
		} else if (commentBlock && contains(line,"*/")) {
			commentBlock = false;
		} else if (!(trim(line) == "")) {
			ruleLength += 1;
		}
	}
	return ruleLength;
}

public int  numberOfRuleBlocks() {	
	return size([1 | /ruleSet(list[Type] selector, list[Declaration] declarations) := stylesheetAST]);
}

public real numberOfAttributesDefinedPerRuleBlock() {	
	return toReal(
	toReal(size([1 | /declaration(str property, list[Type] values) := stylesheetAST]))
	/
	toReal(size([1 | /ruleSet(list[Expression] selector, list[Declaration] declarations) := stylesheetAST]))
	);
	//return toReal(size([1 | /ruleSet(list[Expression] selector, list[Declaration] declarations) := stylesheetAST]));
}

public int numberOfCohesiveRuleBlocks() =
	return size([1 | /ruleSet(list[Type] selector, list[Declaration] declarations) := stylesheetAST, size(declarations) == 1]);
}