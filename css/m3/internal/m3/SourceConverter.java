package org.rascalmpl.library.lang.css.m3.internal.m3;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.rascalmpl.interpreter.IEvaluatorContext;
import org.rascalmpl.interpreter.env.Pair;
import org.rascalmpl.value.ISourceLocation;
import org.rascalmpl.value.type.TypeStore;

import cz.vutbr.web.css.CSSComment;
import cz.vutbr.web.css.CSSNodeVisitor;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.MediaExpression;
import cz.vutbr.web.css.MediaQuery;
import cz.vutbr.web.css.Rule;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleCharset;
import cz.vutbr.web.css.RuleCounterStyle;
import cz.vutbr.web.css.RuleFontFace;
import cz.vutbr.web.css.RuleImport;
import cz.vutbr.web.css.RuleKeyframes;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleNameSpace;
import cz.vutbr.web.css.RulePage;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.RuleViewport;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.Selector.ElementAttribute;
import cz.vutbr.web.css.Selector.ElementClass;
import cz.vutbr.web.css.Selector.ElementID;
import cz.vutbr.web.css.Selector.ElementName;
import cz.vutbr.web.css.Selector.KeyframesIdent;
import cz.vutbr.web.css.Selector.KeyframesPercentage;
import cz.vutbr.web.css.Selector.PseudoPage;
import cz.vutbr.web.css.Selector.SelectorPart;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.css.TermAngle;
import cz.vutbr.web.css.TermAudio;
import cz.vutbr.web.css.TermCalc;
import cz.vutbr.web.css.TermColor;
import cz.vutbr.web.css.TermExpression;
import cz.vutbr.web.css.TermFrequency;
import cz.vutbr.web.css.TermFunction;
import cz.vutbr.web.css.TermIdent;
import cz.vutbr.web.css.TermInteger;
import cz.vutbr.web.css.TermLength;
import cz.vutbr.web.css.TermNumber;
import cz.vutbr.web.css.TermPercent;
import cz.vutbr.web.css.TermResolution;
import cz.vutbr.web.css.TermString;
import cz.vutbr.web.css.TermTime;
import cz.vutbr.web.css.TermURI;

public class SourceConverter extends M3Converter implements CSSNodeVisitor {

	private String stylesheetName;

	private boolean fontfaceUsesRelationAllowed = true;
	private List<Pair<String, ISourceLocation>> fontFaceRules;
	private List<Pair<Declaration, ISourceLocation>> fontDeclarations;

	private boolean keyFramesUsesRelationAllowed = true;
	private List<Pair<String, ISourceLocation>> keyFramesRules;
	private List<Pair<Declaration, ISourceLocation>> animationDeclarations;

	List<ISourceLocation> bindingLocations;
	ISourceLocation commentTarget;

	public SourceConverter(TypeStore typeStore, Map<String, ISourceLocation> cache, ISourceLocation loc,
			IEvaluatorContext eval) {
		super(typeStore, cache, loc, eval);

		bindingLocations = new ArrayList<>();

		fontFaceRules = new ArrayList<Pair<String, ISourceLocation>>();
		fontDeclarations = new ArrayList<Pair<Declaration, ISourceLocation>>();

		keyFramesRules = new ArrayList<Pair<String, ISourceLocation>>();
		animationDeclarations = new ArrayList<Pair<Declaration, ISourceLocation>>();
	}

	public void convert(StyleSheet rules) {
		this.stylesheet = rules;

		stylesheetName = rules.getName();

		rules.accept(this);

		createUsesRelations(fontFaceRules, fontDeclarations);
		createUsesRelations(keyFramesRules, animationDeclarations);
	}

	private void createUsesRelations(List<Pair<String, ISourceLocation>> rules,
			List<Pair<Declaration, ISourceLocation>> declarations) {
		for (Pair<String, ISourceLocation> rule : rules) {
			for (Pair<Declaration, ISourceLocation> decl : declarations) {
				if (decl.getFirst().toString().contains(rule.getFirst())) {
					insert(uses, decl.getSecond(), rule.getSecond());
				}
			}
		}
	}

	@Override
	public Void visit(Declaration node) {
		// eval.getStdOut().println("Declaration");
		// eval.getStdOut().println("\t" + node.getProperty());

		// makeBinding("css+declaration", null, node.getProperty());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+declaration", null,
				stylesheetName + "/" + node.getProperty());
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		if ((node.getProperty().equals("font") || node.getProperty().equals("font-family")) && fontfaceUsesRelationAllowed) {
			fontDeclarations.add(new Pair<Declaration, ISourceLocation>(node, bindedLocation));
		} else if ((node.getProperty().equals("animation") || node.getProperty().equals("animation-name")) && keyFramesUsesRelationAllowed) {
			animationDeclarations.add(new Pair<Declaration, ISourceLocation>(node, bindedLocation));
		}

		scopeManager.push(bindedLocation);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		if (node.isImportant()) {
			String modifier = "important";
			insert(modifiers, nodeLocation, constructModifierNode(modifier));
		}

		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(CombinedSelector node) {
		// eval.getStdOut().println("CombinedSelector");

		// makeBinding("css+selector", null, node.toString());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		String selector = formatSelectorForPresentation(node.toString());

		ISourceLocation bindedLocation = makeBinding("css+selector", null, stylesheetName + "/" + selector);
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		scopeManager.push(bindedLocation);

		for (Iterator<Selector> it = node.iterator(); it.hasNext();) {
			Selector s = it.next();
			s.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(MediaExpression node) {
		// eval.getStdOut().println("MediaExpression");
		// eval.getStdOut().println(node.getFeature());

		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(MediaQuery node) {
		// eval.getStdOut().println("MediaQuery");
		// eval.getStdOut().println(node.getType());

		for (Iterator<MediaExpression> it = node.iterator(); it.hasNext();) {
			MediaExpression m = it.next();
			m.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(RuleFontFace node) {
		// eval.getStdOut().println("RuleFontFace");

		String fontTitle = "";

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();

			if (d.getProperty().equals("font-family")) {
				fontTitle = d.asList().toString();
				fontTitle = fontTitle.substring(1, fontTitle.length() - 1);
				break;
			}
		}

		// makeBinding("css+fontfacerule", null, fontTitle);
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+fontfacerule", null, stylesheetName + "/" + fontTitle);
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		fontFaceRules.add(new Pair<String, ISourceLocation>(fontTitle, bindedLocation));

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		scopeManager.push(bindedLocation);
		
		fontfaceUsesRelationAllowed = false;

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			d.accept(this);
		}
		
		fontfaceUsesRelationAllowed = true;

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(RuleMedia node) {
		// eval.getStdOut().println("RuleMedia");

		// makeBinding("css+mediarule", null, "RULEMEDIA");
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+mediarule", null,
				stylesheetName + "/" + node.getMediaQueries().toString());
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		scopeManager.push(bindedLocation);

		for (Iterator<MediaQuery> it = node.getMediaQueries().iterator(); it.hasNext();) {
			MediaQuery m = it.next();
			m.accept(this);
		}

		for (Iterator<RuleSet> it = node.iterator(); it.hasNext();) {
			RuleSet r = it.next();
			r.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(RulePage node) {
		// eval.getStdOut().println("RulePage");

		// makeBinding("css+pagerule", null, "RULEPAGE");
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+pagerule", null, stylesheetName + "/" + node.getName());
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		scopeManager.push(bindedLocation);

		for (Iterator<Rule<?>> it = node.iterator(); it.hasNext();) {
			Rule<?> r = it.next();
			r.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(RuleSet node) {
		// eval.getStdOut().println("RuleSet");

		String selectors = "";

		for (CombinedSelector cs : node.getSelectors()) {
			String selector = formatSelectorForPresentation(cs.toString());

			selectors += selector + ",";
		}

		selectors = selectors.substring(0, selectors.length() - 1);

		// makeBinding("css+ruleset", null, selectors);
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+ruleset", null, stylesheetName + "/" + selectors);
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		scopeManager.push(bindedLocation);

		for (CombinedSelector cs : node.getSelectors()) {
			cs.accept(this);
		}

		for (Declaration cs : node) {
			cs.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(RuleViewport node) {
		// eval.getStdOut().println("RuleViewport");

		String declarationsKey = "";

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			declarationsKey += d.toString();
		}

		declarationsKey = declarationsKey.substring(0, declarationsKey.length() - 1);

		// makeBinding("css+viewportrule", null, declarations);
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+viewportrule", null, stylesheetName + "/" + declarationsKey);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		scopeManager.push(bindedLocation);

		for (Iterator<Declaration> it = node.iterator(); it.hasNext();) {
			Declaration d = it.next();
			d.accept(this);
		}

		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(Selector node) {
		// eval.getStdOut().println("Selector");
		// eval.getStdOut().println("\t" + node.getCombinator());

		for (Iterator<SelectorPart> it = node.iterator(); it.hasNext();) {
			SelectorPart m = it.next();
			m.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(StyleSheet node) {
		// eval.getStdOut().println("StyleSheet");

		// makeBinding("css+stylesheet", null, "style1.css");
		ownValue = loc;

		ISourceLocation bindedLocation = makeBinding("css+stylesheet", null, loc.getPath());

		scopeManager.push(bindedLocation);

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		for (Iterator<RuleBlock<?>> it = node.iterator(); it.hasNext();) {
			RuleBlock<?> r = it.next();
			r.accept(this);
		}

		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		insert(declarations, bindedLocation, nodeLocation);
		insert(names, values.string(stylesheetName.replaceAll(".css$", "")), bindedLocation);
		
		scopeManager.pop();

		return null;
	}

	@Override
	public Void visit(TermAngle node) {
		// eval.getStdOut().println("TermAngle");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermColor node) {
		// eval.getStdOut().println("TermColor");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Void visit(TermExpression node) {
		// eval.getStdOut().println("TermExpression");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Void visit(TermFrequency node) {
		// eval.getStdOut().println("TermFrequency");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermFunction node) {
		// eval.getStdOut().println("TermFunction");
		// eval.getStdOut().println(node.getFunctionName());

		for (Iterator<Term<?>> it = node.iterator(); it.hasNext();) {
			Term<?> t = it.next();
			t.accept(this);
		}

		return null;
	}

	@Override
	public Void visit(TermIdent node) {
		// eval.getStdOut().println("TermIdent");
		// eval.getStdOut().println("\t" + node.getValue());

		return null;
	}

	@Override
	public Void visit(TermInteger node) {
		// eval.getStdOut().println("TermInteger");
		// For some strange reason termInteger contains floats...
		// eval.getStdOut().println("\t" + node.getValue().intValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermLength node) {
		// eval.getStdOut().println("TermLength");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermNumber node) {
		// eval.getStdOut().println("TermNumber");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermPercent node) {
		// eval.getStdOut().println("TermPercent");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermResolution node) {
		// eval.getStdOut().println("TermResolution");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermString node) {
		// eval.getStdOut().println("TermString");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Void visit(TermTime node) {
		// eval.getStdOut().println("TermTime");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Void visit(TermURI node) {
		// eval.getStdOut().println("TermURI");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Void visit(ElementAttribute node) {
		// eval.getStdOut().println("ElementAttribute");
		// eval.getStdOut().println("\t" + node.getAttribute() + " " +
		// node.getOperator() + " " + node.getValue());
		return null;
	}

	@Override
	public Void visit(ElementClass node) {
		// eval.getStdOut().println("ElementClass");
		// eval.getStdOut().println("\t" + node.getClassName());

		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+class", null,
				stylesheetName + "/" + node.getClassName().substring(1));

		insert(names, values.string(node.getClassName().substring(1, node.getClassName().length())), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Void visit(ElementID node) {
		// eval.getStdOut().println("ElementID");
		// eval.getStdOut().println("\t" + node.getID());

		// makeBinding("css+id", null, node.getID());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+id", null, stylesheetName + "/" + node.getID().substring(1));

		insert(names, values.string(node.getID().substring(1, node.getID().length())), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Void visit(ElementName node) {
		// eval.getStdOut().println("ElementName");
		// eval.getStdOut().println("\t" + node.getName());
		return null;
	}

	@Override
	public Void visit(PseudoPage node) {
		// eval.getStdOut().println("PseudoPage");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Object visit(RuleImport node) {
		// eval.getStdOut().println("RuleImport");

		// makeBinding("css+importrule", null, node.getURI());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+importrule", null, stylesheetName + "/" + node.getURI());

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Object visit(CSSComment node) {
		// eval.getStdOut().println("CSSComment");

		// eval.getStdOut().println(node.getText());

		// eval.getStdOut().println("Parent: "+getParent().getPath());

		// makeBinding("css+comment", null, node.getText());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		// ISourceLocation bindedLocation = makeBinding("css+comment", null,
		// node.getText());

		insert(documentation, commentTarget, nodeLocation);
		// insert(containment, commentTarget, nodeLocation);
		// insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	private ISourceLocation checkBinding(String scheme, String authority, String path, int i) {
		ISourceLocation loc;
		try {
			if (i == 0) {
				loc = values.sourceLocation(scheme, authority, path);
			} else {
				loc = values.sourceLocation(scheme, authority, path + "(" + i + ")");
			}
			if (bindingLocations.contains(loc)) {
				int a = i + 1;
				return checkBinding(scheme, authority, path, a);
			} else {
				return loc;
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	protected ISourceLocation makeBinding(String scheme, String authority, String path) {
		ISourceLocation loc = checkBinding(scheme, authority, path, 0);
		bindingLocations.add(loc);
		return loc;
	}

	@Override
	public Object visit(RuleCharset node) {
		// eval.getStdOut().println("RuleCharset");

		// makeBinding("css+importrule", null, node.getURI());
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+charsetrule", null, stylesheetName + "/" + node.getCharset());

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Object visit(TermCalc node) {
		// eval.getStdOut().println("TermCalc");
		// eval.getStdOut().println("\t" + node.getValue());
		return null;
	}

	@Override
	public Object visit(RuleCounterStyle node) {
		// eval.getStdOut().println("RuleCounterStyle");

		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+counterstylerule", null,
				stylesheetName + "/" + node.getName());

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Object visit(RuleNameSpace node) {
		// eval.getStdOut().println("RuleNameSpace");

		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation;
		if (node.getPrefix().equals("")) {
			bindedLocation = makeBinding("css+namespacerule", null, stylesheetName + "/" + node.getUri());
		} else {
			bindedLocation = makeBinding("css+namespacerule", null,
					stylesheetName + "/" + node.getPrefix() + "-" + node.getUri());
		}

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		return null;
	}

	@Override
	public Object visit(TermAudio node) {
		// eval.getStdOut().println("TermAudio");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Object visit(KeyframesPercentage node) {
		// eval.getStdOut().println("keyframesPercentage");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Object visit(KeyframesIdent node) {
		// eval.getStdOut().println("keyframesIdent");
		// eval.getStdOut().println("\t" + node.getValue() + " " +
		// node.getUnit());
		return null;
	}

	@Override
	public Object visit(RuleKeyframes node) {
		// eval.getStdOut().println("RuleMedia");

		// makeBinding("css+mediarule", null, "RULEMEDIA");
		ISourceLocation nodeLocation = createLocation(loc, node.getLocation());
		ownValue = nodeLocation;

		ISourceLocation bindedLocation = makeBinding("css+keyframesrule", null, stylesheetName + "/" + node.getName());
		insert(containment, getParent(), bindedLocation);
		insert(declarations, bindedLocation, nodeLocation);

		keyFramesRules.add(new Pair<String, ISourceLocation>(node.getName(), bindedLocation));

		commentTarget = bindedLocation;
		if (node.getComment() != null) {
			node.getComment().accept(this);
		}

		scopeManager.push(bindedLocation);
		
		keyFramesUsesRelationAllowed = false;

		for (Iterator<RuleSet> it = node.iterator(); it.hasNext();) {
			RuleSet r = it.next();
			r.accept(this);
		}
		
		keyFramesUsesRelationAllowed = true;

		scopeManager.pop();

		return null;
	}

	private String formatSelectorForPresentation(String selector) {
		selector = selector.replaceAll("[.]", "class:");
		selector = selector.replaceAll("[#]", "id:");

		selector = selector.replaceAll("[+]", "(AS)"); // adjacent sibling
														// selector
		selector = selector.replaceAll("[>]", "(CH)"); // child selector
		selector = selector.replaceAll("[~]", "(GS)"); // general sibling
														// selector
		selector = selector.replaceAll(" ", "(DS)"); // descendant selector
		return selector;
	}

}
