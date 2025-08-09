// Generated from org/taylorlang/grammar/TaylorLang.g4 by ANTLR 4.13.1

package org.taylorlang.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TaylorLangParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
<<<<<<< HEAD
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, BooleanLiteral=48, IntLiteral=49, FloatLiteral=50, 
		StringLiteral=51, IDENTIFIER=52, LineComment=53, BlockComment=54, WS=55;
	public static final int
		RULE_program = 0, RULE_statement = 1, RULE_functionDecl = 2, RULE_functionBody = 3, 
		RULE_paramList = 4, RULE_param = 5, RULE_typeDecl = 6, RULE_unionType = 7, 
		RULE_productType = 8, RULE_positionedProductType = 9, RULE_namedProductType = 10, 
		RULE_namedFieldList = 11, RULE_namedField = 12, RULE_positionedFieldList = 13, 
		RULE_valDecl = 14, RULE_typeParams = 15, RULE_typeParam = 16, RULE_type = 17, 
		RULE_typeArgs = 18, RULE_primitiveType = 19, RULE_expression = 20, RULE_primary = 21, 
		RULE_blockExpr = 22, RULE_blockContent = 23, RULE_literal = 24, RULE_tupleLiteral = 25, 
		RULE_constructorCall = 26, RULE_argList = 27, RULE_ifExpr = 28, RULE_forExpr = 29, 
		RULE_matchExpr = 30, RULE_matchCase = 31, RULE_pattern = 32, RULE_constructorPattern = 33, 
		RULE_lambdaExpr = 34;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "statement", "functionDecl", "functionBody", "paramList", 
			"param", "typeDecl", "unionType", "productType", "positionedProductType", 
			"namedProductType", "namedFieldList", "namedField", "positionedFieldList", 
			"valDecl", "typeParams", "typeParam", "type", "typeArgs", "primitiveType", 
			"expression", "primary", "blockExpr", "blockContent", "literal", "tupleLiteral", 
			"constructorCall", "argList", "ifExpr", "forExpr", "matchExpr", "matchCase", 
=======
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, IDENTIFIER=44, IntLiteral=45, 
		FloatLiteral=46, StringLiteral=47, BooleanLiteral=48, LineComment=49, 
		BlockComment=50, WS=51;
	public static final int
		RULE_program = 0, RULE_statement = 1, RULE_functionDecl = 2, RULE_functionBody = 3, 
		RULE_paramList = 4, RULE_param = 5, RULE_typeDecl = 6, RULE_unionType = 7, 
		RULE_productType = 8, RULE_fieldList = 9, RULE_field = 10, RULE_valDecl = 11, 
		RULE_typeParams = 12, RULE_typeParam = 13, RULE_type = 14, RULE_typeArgs = 15, 
		RULE_primitiveType = 16, RULE_expression = 17, RULE_primary = 18, RULE_literal = 19, 
		RULE_listLiteral = 20, RULE_mapLiteral = 21, RULE_mapEntry = 22, RULE_tupleLiteral = 23, 
		RULE_constructorCall = 24, RULE_argList = 25, RULE_matchExpr = 26, RULE_matchCase = 27, 
		RULE_pattern = 28, RULE_constructorPattern = 29, RULE_lambdaExpr = 30;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "statement", "functionDecl", "functionBody", "paramList", 
			"param", "typeDecl", "unionType", "productType", "fieldList", "field", 
			"valDecl", "typeParams", "typeParam", "type", "typeArgs", "primitiveType", 
			"expression", "primary", "literal", "listLiteral", "mapLiteral", "mapEntry", 
			"tupleLiteral", "constructorCall", "argList", "matchExpr", "matchCase", 
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			"pattern", "constructorPattern", "lambdaExpr"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'fn'", "'('", "')'", "':'", "'=>'", "'{'", "'}'", "','", "'type'", 
			"'='", "'|'", "'val'", "'<'", "'>'", "'?'", "'Int'", "'Long'", "'Float'", 
			"'Double'", "'Boolean'", "'String'", "'Unit'", "'.'", "'['", "']'", "'-'", 
			"'!'", "'*'", "'/'", "'%'", "'+'", "'<='", "'>='", "'=='", "'!='", "'&&'", 
<<<<<<< HEAD
			"'||'", "'?:'", "';'", "'null'", "'if'", "'else'", "'for'", "'in'", "'match'", 
			"'case'", "'_'"
=======
			"'||'", "'?:'", "'null'", "'match'", "'case'", "'_'", "'if'"
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
<<<<<<< HEAD
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"BooleanLiteral", "IntLiteral", "FloatLiteral", "StringLiteral", "IDENTIFIER", 
			"LineComment", "BlockComment", "WS"
=======
			null, null, null, null, null, null, null, null, "IDENTIFIER", "IntLiteral", 
			"FloatLiteral", "StringLiteral", "BooleanLiteral", "LineComment", "BlockComment", 
			"WS"
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TaylorLang.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TaylorLangParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(TaylorLangParser.EOF, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(73);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8773003479355974L) != 0)) {
				{
				{
				setState(70);
				statement();
				}
				}
				setState(75);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(76);
=======
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252927046L) != 0)) {
				{
				{
				setState(62);
				statement();
				}
				}
				setState(67);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(68);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public FunctionDeclContext functionDecl() {
			return getRuleContext(FunctionDeclContext.class,0);
		}
		public TypeDeclContext typeDecl() {
			return getRuleContext(TypeDeclContext.class,0);
		}
		public ValDeclContext valDecl() {
			return getRuleContext(ValDeclContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		try {
<<<<<<< HEAD
			setState(82);
=======
			setState(74);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				enterOuterAlt(_localctx, 1);
				{
<<<<<<< HEAD
				setState(78);
=======
				setState(70);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				functionDecl();
				}
				break;
			case T__8:
				enterOuterAlt(_localctx, 2);
				{
<<<<<<< HEAD
				setState(79);
=======
				setState(71);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				typeDecl();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 3);
				{
<<<<<<< HEAD
				setState(80);
=======
				setState(72);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				valDecl();
				}
				break;
			case T__1:
			case T__5:
<<<<<<< HEAD
			case T__25:
			case T__26:
			case T__39:
			case T__40:
			case T__42:
			case T__44:
			case BooleanLiteral:
			case IntLiteral:
			case FloatLiteral:
			case StringLiteral:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 4);
				{
				setState(81);
=======
			case T__23:
			case T__25:
			case T__26:
			case T__38:
			case T__39:
			case IDENTIFIER:
			case IntLiteral:
			case FloatLiteral:
			case StringLiteral:
			case BooleanLiteral:
				enterOuterAlt(_localctx, 4);
				{
				setState(73);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				expression(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionDeclContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public TypeParamsContext typeParams() {
			return getRuleContext(TypeParamsContext.class,0);
		}
		public ParamListContext paramList() {
			return getRuleContext(ParamListContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FunctionDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterFunctionDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitFunctionDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitFunctionDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclContext functionDecl() throws RecognitionException {
		FunctionDeclContext _localctx = new FunctionDeclContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_functionDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(84);
			match(T__0);
			setState(85);
			match(IDENTIFIER);
			setState(87);
=======
			setState(76);
			match(T__0);
			setState(77);
			match(IDENTIFIER);
			setState(79);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
<<<<<<< HEAD
				setState(86);
=======
				setState(78);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				typeParams();
				}
			}

<<<<<<< HEAD
			setState(89);
			match(T__1);
			setState(91);
=======
			setState(81);
			match(T__1);
			setState(83);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IDENTIFIER) {
				{
<<<<<<< HEAD
				setState(90);
=======
				setState(82);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				paramList();
				}
			}

<<<<<<< HEAD
			setState(93);
			match(T__2);
			setState(96);
=======
			setState(85);
			match(T__2);
			setState(88);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__3) {
				{
<<<<<<< HEAD
				setState(94);
				match(T__3);
				setState(95);
=======
				setState(86);
				match(T__3);
				setState(87);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				type(0);
				}
			}

<<<<<<< HEAD
			setState(98);
			match(T__4);
			setState(99);
=======
			setState(90);
			match(T__4);
			setState(91);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			functionBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionBodyContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public FunctionBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterFunctionBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitFunctionBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitFunctionBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionBodyContext functionBody() throws RecognitionException {
		FunctionBodyContext _localctx = new FunctionBodyContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_functionBody);
		int _la;
		try {
<<<<<<< HEAD
			setState(110);
=======
			setState(102);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
<<<<<<< HEAD
				setState(101);
=======
				setState(93);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				expression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
<<<<<<< HEAD
				setState(102);
				match(T__5);
				setState(106);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8773003479355974L) != 0)) {
					{
					{
					setState(103);
					statement();
					}
					}
					setState(108);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(109);
=======
				setState(94);
				match(T__5);
				setState(98);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252927046L) != 0)) {
					{
					{
					setState(95);
					statement();
					}
					}
					setState(100);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(101);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(T__6);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParamListContext extends ParserRuleContext {
		public List<ParamContext> param() {
			return getRuleContexts(ParamContext.class);
		}
		public ParamContext param(int i) {
			return getRuleContext(ParamContext.class,i);
		}
		public ParamListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_paramList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterParamList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitParamList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitParamList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamListContext paramList() throws RecognitionException {
		ParamListContext _localctx = new ParamListContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_paramList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(112);
			param();
			setState(117);
=======
			setState(104);
			param();
			setState(109);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
<<<<<<< HEAD
				setState(113);
				match(T__7);
				setState(114);
				param();
				}
				}
				setState(119);
=======
				setState(105);
				match(T__7);
				setState(106);
				param();
				}
				}
				setState(111);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParamContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_param; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParamContext param() throws RecognitionException {
		ParamContext _localctx = new ParamContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_param);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(120);
			match(IDENTIFIER);
			setState(123);
=======
			setState(112);
			match(IDENTIFIER);
			setState(115);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__3) {
				{
<<<<<<< HEAD
				setState(121);
				match(T__3);
				setState(122);
=======
				setState(113);
				match(T__3);
				setState(114);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				type(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDeclContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public UnionTypeContext unionType() {
			return getRuleContext(UnionTypeContext.class,0);
		}
		public TypeParamsContext typeParams() {
			return getRuleContext(TypeParamsContext.class,0);
		}
		public TypeDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterTypeDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitTypeDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitTypeDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclContext typeDecl() throws RecognitionException {
		TypeDeclContext _localctx = new TypeDeclContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_typeDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(125);
			match(T__8);
			setState(126);
			match(IDENTIFIER);
			setState(128);
=======
			setState(117);
			match(T__8);
			setState(118);
			match(IDENTIFIER);
			setState(120);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
<<<<<<< HEAD
				setState(127);
=======
				setState(119);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				typeParams();
				}
			}

<<<<<<< HEAD
			setState(130);
			match(T__9);
			setState(131);
=======
			setState(122);
			match(T__9);
			setState(123);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			unionType();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnionTypeContext extends ParserRuleContext {
		public List<ProductTypeContext> productType() {
			return getRuleContexts(ProductTypeContext.class);
		}
		public ProductTypeContext productType(int i) {
			return getRuleContext(ProductTypeContext.class,i);
		}
		public UnionTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterUnionType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitUnionType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitUnionType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnionTypeContext unionType() throws RecognitionException {
		UnionTypeContext _localctx = new UnionTypeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_unionType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(133);
			productType();
			setState(138);
=======
			setState(125);
			productType();
			setState(130);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__10) {
				{
				{
<<<<<<< HEAD
				setState(134);
				match(T__10);
				setState(135);
				productType();
				}
				}
				setState(140);
=======
				setState(126);
				match(T__10);
				setState(127);
				productType();
				}
				}
				setState(132);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProductTypeContext extends ParserRuleContext {
<<<<<<< HEAD
		public PositionedProductTypeContext positionedProductType() {
			return getRuleContext(PositionedProductTypeContext.class,0);
		}
		public NamedProductTypeContext namedProductType() {
			return getRuleContext(NamedProductTypeContext.class,0);
=======
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public FieldListContext fieldList() {
			return getRuleContext(FieldListContext.class,0);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		}
		public ProductTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_productType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterProductType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitProductType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitProductType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProductTypeContext productType() throws RecognitionException {
		ProductTypeContext _localctx = new ProductTypeContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_productType);
		try {
<<<<<<< HEAD
			setState(143);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(141);
				positionedProductType();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(142);
				namedProductType();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PositionedProductTypeContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public PositionedFieldListContext positionedFieldList() {
			return getRuleContext(PositionedFieldListContext.class,0);
		}
		public PositionedProductTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionedProductType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterPositionedProductType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitPositionedProductType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitPositionedProductType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionedProductTypeContext positionedProductType() throws RecognitionException {
		PositionedProductTypeContext _localctx = new PositionedProductTypeContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_positionedProductType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			match(IDENTIFIER);
			setState(150);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(146);
				match(T__1);
				setState(147);
				positionedFieldList();
				setState(148);
=======
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			match(IDENTIFIER);
			setState(138);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(134);
				match(T__1);
				setState(135);
				fieldList();
				setState(136);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(T__2);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
<<<<<<< HEAD
	public static class NamedProductTypeContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public NamedFieldListContext namedFieldList() {
			return getRuleContext(NamedFieldListContext.class,0);
		}
		public NamedProductTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedProductType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterNamedProductType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitNamedProductType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitNamedProductType(this);
=======
	public static class FieldListContext extends ParserRuleContext {
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public FieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitFieldList(this);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			else return visitor.visitChildren(this);
		}
	}

<<<<<<< HEAD
	public final NamedProductTypeContext namedProductType() throws RecognitionException {
		NamedProductTypeContext _localctx = new NamedProductTypeContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_namedProductType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			match(IDENTIFIER);
			setState(153);
			match(T__1);
			setState(154);
			namedFieldList();
			setState(155);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamedFieldListContext extends ParserRuleContext {
		public List<NamedFieldContext> namedField() {
			return getRuleContexts(NamedFieldContext.class);
		}
		public NamedFieldContext namedField(int i) {
			return getRuleContext(NamedFieldContext.class,i);
		}
		public NamedFieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedFieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterNamedFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitNamedFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitNamedFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedFieldListContext namedFieldList() throws RecognitionException {
		NamedFieldListContext _localctx = new NamedFieldListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_namedFieldList);
=======
	public final FieldListContext fieldList() throws RecognitionException {
		FieldListContext _localctx = new FieldListContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_fieldList);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(157);
			namedField();
			setState(162);
=======
			setState(140);
			field();
			setState(145);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
<<<<<<< HEAD
				setState(158);
				match(T__7);
				setState(159);
				namedField();
				}
				}
				setState(164);
=======
				setState(141);
				match(T__7);
				setState(142);
				field();
				}
				}
				setState(147);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
<<<<<<< HEAD
	public static class NamedFieldContext extends ParserRuleContext {
=======
	public static class FieldContext extends ParserRuleContext {
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
<<<<<<< HEAD
		public NamedFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterNamedField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitNamedField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitNamedField(this);
=======
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitField(this);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			else return visitor.visitChildren(this);
		}
	}

<<<<<<< HEAD
	public final NamedFieldContext namedField() throws RecognitionException {
		NamedFieldContext _localctx = new NamedFieldContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_namedField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			match(IDENTIFIER);
			setState(166);
			match(T__3);
			setState(167);
=======
	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_field);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(IDENTIFIER);
			setState(149);
			match(T__3);
			setState(150);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			type(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
<<<<<<< HEAD
	public static class PositionedFieldListContext extends ParserRuleContext {
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public PositionedFieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionedFieldList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterPositionedFieldList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitPositionedFieldList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitPositionedFieldList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionedFieldListContext positionedFieldList() throws RecognitionException {
		PositionedFieldListContext _localctx = new PositionedFieldListContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_positionedFieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(169);
			type(0);
			setState(174);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
				setState(170);
				match(T__7);
				setState(171);
				type(0);
				}
				}
				setState(176);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	public static class ValDeclContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ValDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterValDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitValDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitValDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValDeclContext valDecl() throws RecognitionException {
		ValDeclContext _localctx = new ValDeclContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 28, RULE_valDecl);
=======
		enterRule(_localctx, 22, RULE_valDecl);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(177);
			match(T__11);
			setState(178);
			match(IDENTIFIER);
			setState(181);
=======
			setState(152);
			match(T__11);
			setState(153);
			match(IDENTIFIER);
			setState(156);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__3) {
				{
<<<<<<< HEAD
				setState(179);
				match(T__3);
				setState(180);
=======
				setState(154);
				match(T__3);
				setState(155);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				type(0);
				}
			}

<<<<<<< HEAD
			setState(183);
			match(T__9);
			setState(184);
=======
			setState(158);
			match(T__9);
			setState(159);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeParamsContext extends ParserRuleContext {
		public List<TypeParamContext> typeParam() {
			return getRuleContexts(TypeParamContext.class);
		}
		public TypeParamContext typeParam(int i) {
			return getRuleContext(TypeParamContext.class,i);
		}
		public TypeParamsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParams; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterTypeParams(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitTypeParams(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitTypeParams(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamsContext typeParams() throws RecognitionException {
		TypeParamsContext _localctx = new TypeParamsContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 30, RULE_typeParams);
=======
		enterRule(_localctx, 24, RULE_typeParams);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(186);
			match(T__12);
			setState(187);
			typeParam();
			setState(192);
=======
			setState(161);
			match(T__12);
			setState(162);
			typeParam();
			setState(167);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
<<<<<<< HEAD
				setState(188);
				match(T__7);
				setState(189);
				typeParam();
				}
				}
				setState(194);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(195);
=======
				setState(163);
				match(T__7);
				setState(164);
				typeParam();
				}
				}
				setState(169);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(170);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(T__13);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeParamContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public TypeParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParam; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterTypeParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitTypeParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitTypeParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamContext typeParam() throws RecognitionException {
		TypeParamContext _localctx = new TypeParamContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 32, RULE_typeParam);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
=======
		enterRule(_localctx, 26, RULE_typeParam);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(172);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public TypeArgsContext typeArgs() {
			return getRuleContext(TypeArgsContext.class,0);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		return type(0);
	}

	private TypeContext type(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TypeContext _localctx = new TypeContext(_ctx, _parentState);
		TypeContext _prevctx = _localctx;
<<<<<<< HEAD
		int _startState = 34;
		enterRecursionRule(_localctx, 34, RULE_type, _p);
=======
		int _startState = 28;
		enterRecursionRule(_localctx, 28, RULE_type, _p);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(218);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(200);
=======
			setState(193);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(175);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				primitiveType();
				}
				break;
			case 2:
				{
<<<<<<< HEAD
				setState(201);
=======
				setState(176);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(IDENTIFIER);
				}
				break;
			case 3:
				{
<<<<<<< HEAD
				setState(202);
				match(IDENTIFIER);
				setState(203);
				match(T__12);
				setState(204);
				typeArgs();
				setState(205);
=======
				setState(177);
				match(IDENTIFIER);
				setState(178);
				match(T__12);
				setState(179);
				typeArgs();
				setState(180);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(T__13);
				}
				break;
			case 4:
				{
<<<<<<< HEAD
				setState(207);
				match(T__1);
				setState(208);
				type(0);
				setState(213);
=======
				setState(182);
				match(T__1);
				setState(183);
				type(0);
				setState(188);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
<<<<<<< HEAD
					setState(209);
					match(T__7);
					setState(210);
					type(0);
					}
					}
					setState(215);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(216);
=======
					setState(184);
					match(T__7);
					setState(185);
					type(0);
					}
					}
					setState(190);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(191);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(T__2);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
<<<<<<< HEAD
			setState(224);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
=======
			setState(199);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new TypeContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_type);
<<<<<<< HEAD
					setState(220);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(221);
=======
					setState(195);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(196);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
					match(T__14);
					}
					} 
				}
<<<<<<< HEAD
				setState(226);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
=======
				setState(201);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeArgsContext extends ParserRuleContext {
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public TypeArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterTypeArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitTypeArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitTypeArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgsContext typeArgs() throws RecognitionException {
		TypeArgsContext _localctx = new TypeArgsContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 36, RULE_typeArgs);
=======
		enterRule(_localctx, 30, RULE_typeArgs);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(227);
			type(0);
			setState(232);
=======
			setState(202);
			type(0);
			setState(207);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
<<<<<<< HEAD
				setState(228);
				match(T__7);
				setState(229);
				type(0);
				}
				}
				setState(234);
=======
				setState(203);
				match(T__7);
				setState(204);
				type(0);
				}
				}
				setState(209);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimitiveTypeContext extends ParserRuleContext {
		public PrimitiveTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primitiveType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterPrimitiveType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitPrimitiveType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitPrimitiveType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimitiveTypeContext primitiveType() throws RecognitionException {
		PrimitiveTypeContext _localctx = new PrimitiveTypeContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 38, RULE_primitiveType);
=======
		enterRule(_localctx, 32, RULE_primitiveType);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(235);
=======
			setState(210);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 8323072L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public PrimaryContext primary() {
			return getRuleContext(PrimaryContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
<<<<<<< HEAD
		public IfExprContext ifExpr() {
			return getRuleContext(IfExprContext.class,0);
		}
		public ForExprContext forExpr() {
			return getRuleContext(ForExprContext.class,0);
		}
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		public MatchExprContext matchExpr() {
			return getRuleContext(MatchExprContext.class,0);
		}
		public LambdaExprContext lambdaExpr() {
			return getRuleContext(LambdaExprContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public ArgListContext argList() {
			return getRuleContext(ArgListContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
<<<<<<< HEAD
		int _startState = 40;
		enterRecursionRule(_localctx, 40, RULE_expression, _p);
=======
		int _startState = 34;
		enterRecursionRule(_localctx, 34, RULE_expression, _p);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(247);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(238);
=======
			setState(220);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				{
				setState(213);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				primary();
				}
				break;
			case 2:
				{
<<<<<<< HEAD
				setState(239);
				match(T__25);
				setState(240);
				expression(12);
=======
				setState(214);
				match(T__25);
				setState(215);
				expression(10);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				}
				break;
			case 3:
				{
<<<<<<< HEAD
				setState(241);
				match(T__26);
				setState(242);
				expression(11);
=======
				setState(216);
				match(T__26);
				setState(217);
				expression(9);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				}
				break;
			case 4:
				{
<<<<<<< HEAD
				setState(243);
				ifExpr();
=======
				setState(218);
				matchExpr();
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				}
				break;
			case 5:
				{
<<<<<<< HEAD
				setState(244);
				forExpr();
				}
				break;
			case 6:
				{
				setState(245);
				matchExpr();
				}
				break;
			case 7:
				{
				setState(246);
=======
				setState(219);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				lambdaExpr();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
<<<<<<< HEAD
			setState(283);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
=======
			setState(256);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
<<<<<<< HEAD
					setState(281);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
=======
					setState(254);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(249);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(250);
=======
						setState(222);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(223);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1879048192L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
<<<<<<< HEAD
						setState(251);
						expression(11);
=======
						setState(224);
						expression(9);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(252);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(253);
=======
						setState(225);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(226);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						_la = _input.LA(1);
						if ( !(_la==T__25 || _la==T__30) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
<<<<<<< HEAD
						setState(254);
						expression(10);
=======
						setState(227);
						expression(8);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(255);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(256);
=======
						setState(228);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(229);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 64424534016L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
<<<<<<< HEAD
						setState(257);
						expression(9);
=======
						setState(230);
						expression(7);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(258);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(259);
						match(T__35);
						setState(260);
						expression(8);
=======
						setState(231);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(232);
						match(T__35);
						setState(233);
						expression(6);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(261);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(262);
						match(T__36);
						setState(263);
						expression(7);
=======
						setState(234);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(235);
						match(T__36);
						setState(236);
						expression(5);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(264);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(265);
						match(T__37);
						setState(266);
						expression(6);
=======
						setState(237);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(238);
						match(T__37);
						setState(239);
						expression(4);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						}
						break;
					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(267);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(268);
						match(T__22);
						setState(269);
=======
						setState(240);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(241);
						match(T__22);
						setState(242);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						match(IDENTIFIER);
						}
						break;
					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(270);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(271);
						match(T__1);
						setState(273);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8773003479351364L) != 0)) {
							{
							setState(272);
=======
						setState(243);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(244);
						match(T__1);
						setState(246);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252922436L) != 0)) {
							{
							setState(245);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
							argList();
							}
						}

<<<<<<< HEAD
						setState(275);
=======
						setState(248);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						match(T__2);
						}
						break;
					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
<<<<<<< HEAD
						setState(276);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(277);
						match(T__23);
						setState(278);
						expression(0);
						setState(279);
=======
						setState(249);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(250);
						match(T__23);
						setState(251);
						expression(0);
						setState(252);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						match(T__24);
						}
						break;
					}
					} 
				}
<<<<<<< HEAD
				setState(285);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
=======
				setState(258);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ConstructorCallContext constructorCall() {
			return getRuleContext(ConstructorCallContext.class,0);
		}
<<<<<<< HEAD
		public BlockExprContext blockExpr() {
			return getRuleContext(BlockExprContext.class,0);
		}
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		public PrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterPrimary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitPrimary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitPrimary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryContext primary() throws RecognitionException {
		PrimaryContext _localctx = new PrimaryContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 42, RULE_primary);
		try {
			setState(294);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(286);
=======
		enterRule(_localctx, 36, RULE_primary);
		try {
			setState(266);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(IDENTIFIER);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
<<<<<<< HEAD
				setState(287);
=======
				setState(260);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				literal();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
<<<<<<< HEAD
				setState(288);
				match(T__1);
				setState(289);
				expression(0);
				setState(290);
=======
				setState(261);
				match(T__1);
				setState(262);
				expression(0);
				setState(263);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(T__2);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
<<<<<<< HEAD
				setState(292);
				constructorCall();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(293);
				blockExpr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BlockExprContext extends ParserRuleContext {
		public BlockContentContext blockContent() {
			return getRuleContext(BlockContentContext.class,0);
		}
		public BlockExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blockExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterBlockExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitBlockExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitBlockExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockExprContext blockExpr() throws RecognitionException {
		BlockExprContext _localctx = new BlockExprContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_blockExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			match(T__5);
			setState(297);
			blockContent();
			setState(298);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BlockContentContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public BlockContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blockContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterBlockContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitBlockContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitBlockContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContentContext blockContent() throws RecognitionException {
		BlockContentContext _localctx = new BlockContentContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_blockContent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(305);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(300);
					statement();
					setState(301);
					match(T__38);
					}
					} 
				}
				setState(307);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			}
			setState(309);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8773003479351364L) != 0)) {
				{
				setState(308);
				expression(0);
				}
			}

=======
				setState(265);
				constructorCall();
				}
				break;
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode IntLiteral() { return getToken(TaylorLangParser.IntLiteral, 0); }
		public TerminalNode FloatLiteral() { return getToken(TaylorLangParser.FloatLiteral, 0); }
		public TerminalNode StringLiteral() { return getToken(TaylorLangParser.StringLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(TaylorLangParser.BooleanLiteral, 0); }
<<<<<<< HEAD
=======
		public ListLiteralContext listLiteral() {
			return getRuleContext(ListLiteralContext.class,0);
		}
		public MapLiteralContext mapLiteral() {
			return getRuleContext(MapLiteralContext.class,0);
		}
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		public TupleLiteralContext tupleLiteral() {
			return getRuleContext(TupleLiteralContext.class,0);
		}
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 48, RULE_literal);
		try {
			setState(317);
=======
		enterRule(_localctx, 38, RULE_literal);
		try {
			setState(276);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IntLiteral:
				enterOuterAlt(_localctx, 1);
				{
<<<<<<< HEAD
				setState(311);
=======
				setState(268);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(IntLiteral);
				}
				break;
			case FloatLiteral:
				enterOuterAlt(_localctx, 2);
				{
<<<<<<< HEAD
				setState(312);
=======
				setState(269);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(FloatLiteral);
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 3);
				{
<<<<<<< HEAD
				setState(313);
=======
				setState(270);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(StringLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 4);
				{
<<<<<<< HEAD
				setState(314);
				match(BooleanLiteral);
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 5);
				{
				setState(315);
				tupleLiteral();
				}
				break;
			case T__39:
				enterOuterAlt(_localctx, 6);
				{
				setState(316);
				match(T__39);
=======
				setState(271);
				match(BooleanLiteral);
				}
				break;
			case T__23:
				enterOuterAlt(_localctx, 5);
				{
				setState(272);
				listLiteral();
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 6);
				{
				setState(273);
				mapLiteral();
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 7);
				{
				setState(274);
				tupleLiteral();
				}
				break;
			case T__38:
				enterOuterAlt(_localctx, 8);
				{
				setState(275);
				match(T__38);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
<<<<<<< HEAD
=======
	public static class ListLiteralContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ListLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterListLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitListLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitListLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListLiteralContext listLiteral() throws RecognitionException {
		ListLiteralContext _localctx = new ListLiteralContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_listLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			match(T__23);
			setState(287);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252922436L) != 0)) {
				{
				setState(279);
				expression(0);
				setState(284);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(280);
					match(T__7);
					setState(281);
					expression(0);
					}
					}
					setState(286);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(289);
			match(T__24);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapLiteralContext extends ParserRuleContext {
		public List<MapEntryContext> mapEntry() {
			return getRuleContexts(MapEntryContext.class);
		}
		public MapEntryContext mapEntry(int i) {
			return getRuleContext(MapEntryContext.class,i);
		}
		public MapLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterMapLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitMapLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitMapLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapLiteralContext mapLiteral() throws RecognitionException {
		MapLiteralContext _localctx = new MapLiteralContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_mapLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(291);
			match(T__5);
			setState(300);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252922436L) != 0)) {
				{
				setState(292);
				mapEntry();
				setState(297);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(293);
					match(T__7);
					setState(294);
					mapEntry();
					}
					}
					setState(299);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(302);
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapEntryContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public MapEntryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapEntry; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterMapEntry(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitMapEntry(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitMapEntry(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapEntryContext mapEntry() throws RecognitionException {
		MapEntryContext _localctx = new MapEntryContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_mapEntry);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			expression(0);
			setState(305);
			match(T__3);
			setState(306);
			expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	public static class TupleLiteralContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TupleLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tupleLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterTupleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitTupleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitTupleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TupleLiteralContext tupleLiteral() throws RecognitionException {
		TupleLiteralContext _localctx = new TupleLiteralContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 50, RULE_tupleLiteral);
=======
		enterRule(_localctx, 46, RULE_tupleLiteral);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(319);
			match(T__1);
			setState(320);
			expression(0);
			setState(323); 
=======
			setState(308);
			match(T__1);
			setState(309);
			expression(0);
			setState(312); 
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
<<<<<<< HEAD
				setState(321);
				match(T__7);
				setState(322);
				expression(0);
				}
				}
				setState(325); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__7 );
			setState(327);
=======
				setState(310);
				match(T__7);
				setState(311);
				expression(0);
				}
				}
				setState(314); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__7 );
			setState(316);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructorCallContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public ArgListContext argList() {
			return getRuleContext(ArgListContext.class,0);
		}
		public ConstructorCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterConstructorCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitConstructorCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitConstructorCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorCallContext constructorCall() throws RecognitionException {
		ConstructorCallContext _localctx = new ConstructorCallContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 52, RULE_constructorCall);
=======
		enterRule(_localctx, 48, RULE_constructorCall);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(329);
			match(IDENTIFIER);
			setState(330);
			match(T__1);
			setState(332);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8773003479351364L) != 0)) {
				{
				setState(331);
=======
			setState(318);
			match(IDENTIFIER);
			setState(319);
			match(T__1);
			setState(321);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 547007252922436L) != 0)) {
				{
				setState(320);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				argList();
				}
			}

<<<<<<< HEAD
			setState(334);
=======
			setState(323);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgListContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ArgListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterArgList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitArgList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitArgList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgListContext argList() throws RecognitionException {
		ArgListContext _localctx = new ArgListContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 54, RULE_argList);
=======
		enterRule(_localctx, 50, RULE_argList);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(336);
			expression(0);
			setState(341);
=======
			setState(325);
			expression(0);
			setState(330);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__7) {
				{
				{
<<<<<<< HEAD
				setState(337);
				match(T__7);
				setState(338);
				expression(0);
				}
				}
				setState(343);
=======
				setState(326);
				match(T__7);
				setState(327);
				expression(0);
				}
				}
				setState(332);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
<<<<<<< HEAD
	public static class IfExprContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public IfExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterIfExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitIfExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitIfExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfExprContext ifExpr() throws RecognitionException {
		IfExprContext _localctx = new IfExprContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_ifExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(344);
			match(T__40);
			setState(345);
			match(T__1);
			setState(346);
			expression(0);
			setState(347);
			match(T__2);
			setState(348);
			expression(0);
			setState(351);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				{
				setState(349);
				match(T__41);
				setState(350);
				expression(0);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForExprContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public ForExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterForExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitForExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitForExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForExprContext forExpr() throws RecognitionException {
		ForExprContext _localctx = new ForExprContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_forExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			match(T__42);
			setState(354);
			match(T__1);
			setState(355);
			match(IDENTIFIER);
			setState(356);
			match(T__43);
			setState(357);
			expression(0);
			setState(358);
			match(T__2);
			setState(359);
			expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	public static class MatchExprContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<MatchCaseContext> matchCase() {
			return getRuleContexts(MatchCaseContext.class);
		}
		public MatchCaseContext matchCase(int i) {
			return getRuleContext(MatchCaseContext.class,i);
		}
		public MatchExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterMatchExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitMatchExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitMatchExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchExprContext matchExpr() throws RecognitionException {
		MatchExprContext _localctx = new MatchExprContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 60, RULE_matchExpr);
=======
		enterRule(_localctx, 52, RULE_matchExpr);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(361);
			match(T__44);
			setState(362);
			expression(0);
			setState(363);
			match(T__5);
			setState(365); 
=======
			setState(333);
			match(T__39);
			setState(334);
			expression(0);
			setState(335);
			match(T__5);
			setState(337); 
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
<<<<<<< HEAD
				setState(364);
				matchCase();
				}
				}
				setState(367); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__45 );
			setState(369);
=======
				setState(336);
				matchCase();
				}
				}
				setState(339); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__40 );
			setState(341);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(T__6);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatchCaseContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public MatchCaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchCase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterMatchCase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitMatchCase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitMatchCase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatchCaseContext matchCase() throws RecognitionException {
		MatchCaseContext _localctx = new MatchCaseContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 62, RULE_matchCase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			match(T__45);
			setState(372);
			pattern(0);
			setState(373);
			match(T__4);
			setState(374);
=======
		enterRule(_localctx, 54, RULE_matchCase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(343);
			match(T__40);
			setState(344);
			pattern(0);
			setState(345);
			match(T__4);
			setState(346);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ConstructorPatternContext constructorPattern() {
			return getRuleContext(ConstructorPatternContext.class,0);
		}
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		return pattern(0);
	}

	private PatternContext pattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PatternContext _localctx = new PatternContext(_ctx, _parentState);
		PatternContext _prevctx = _localctx;
<<<<<<< HEAD
		int _startState = 64;
		enterRecursionRule(_localctx, 64, RULE_pattern, _p);
=======
		int _startState = 56;
		enterRecursionRule(_localctx, 56, RULE_pattern, _p);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(381);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				{
				setState(377);
				match(T__46);
=======
			setState(353);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				{
				setState(349);
				match(T__41);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				}
				break;
			case 2:
				{
<<<<<<< HEAD
				setState(378);
=======
				setState(350);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				match(IDENTIFIER);
				}
				break;
			case 3:
				{
<<<<<<< HEAD
				setState(379);
=======
				setState(351);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				literal();
				}
				break;
			case 4:
				{
<<<<<<< HEAD
				setState(380);
=======
				setState(352);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				constructorPattern();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
<<<<<<< HEAD
			setState(388);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
=======
			setState(360);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new PatternContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_pattern);
<<<<<<< HEAD
					setState(383);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(384);
					match(T__40);
					setState(385);
=======
					setState(355);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(356);
					match(T__42);
					setState(357);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
					expression(0);
					}
					} 
				}
<<<<<<< HEAD
				setState(390);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
=======
				setState(362);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstructorPatternContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(TaylorLangParser.IDENTIFIER, 0); }
		public List<PatternContext> pattern() {
			return getRuleContexts(PatternContext.class);
		}
		public PatternContext pattern(int i) {
			return getRuleContext(PatternContext.class,i);
		}
		public ConstructorPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterConstructorPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitConstructorPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitConstructorPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorPatternContext constructorPattern() throws RecognitionException {
		ConstructorPatternContext _localctx = new ConstructorPatternContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 66, RULE_constructorPattern);
=======
		enterRule(_localctx, 58, RULE_constructorPattern);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
<<<<<<< HEAD
			setState(391);
			match(IDENTIFIER);
			setState(392);
			match(T__1);
			setState(401);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8867561278013444L) != 0)) {
				{
				setState(393);
				pattern(0);
				setState(398);
=======
			setState(363);
			match(IDENTIFIER);
			setState(364);
			match(T__1);
			setState(373);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 550305586479172L) != 0)) {
				{
				setState(365);
				pattern(0);
				setState(370);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
<<<<<<< HEAD
					setState(394);
					match(T__7);
					setState(395);
					pattern(0);
					}
					}
					setState(400);
=======
					setState(366);
					match(T__7);
					setState(367);
					pattern(0);
					}
					}
					setState(372);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

<<<<<<< HEAD
			setState(403);
=======
			setState(375);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LambdaExprContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(TaylorLangParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(TaylorLangParser.IDENTIFIER, i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public LambdaExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambdaExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).enterLambdaExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TaylorLangListener ) ((TaylorLangListener)listener).exitLambdaExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TaylorLangVisitor ) return ((TaylorLangVisitor<? extends T>)visitor).visitLambdaExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LambdaExprContext lambdaExpr() throws RecognitionException {
		LambdaExprContext _localctx = new LambdaExprContext(_ctx, getState());
<<<<<<< HEAD
		enterRule(_localctx, 68, RULE_lambdaExpr);
		int _la;
		try {
			setState(422);
=======
		enterRule(_localctx, 60, RULE_lambdaExpr);
		int _la;
		try {
			setState(394);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
<<<<<<< HEAD
				setState(405);
				match(IDENTIFIER);
				setState(406);
				match(T__4);
				setState(407);
=======
				setState(377);
				match(IDENTIFIER);
				setState(378);
				match(T__4);
				setState(379);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				expression(0);
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 2);
				{
<<<<<<< HEAD
				setState(408);
				match(T__1);
				setState(417);
=======
				setState(380);
				match(T__1);
				setState(389);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==IDENTIFIER) {
					{
<<<<<<< HEAD
					setState(409);
					match(IDENTIFIER);
					setState(414);
=======
					setState(381);
					match(IDENTIFIER);
					setState(386);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__7) {
						{
						{
<<<<<<< HEAD
						setState(410);
						match(T__7);
						setState(411);
						match(IDENTIFIER);
						}
						}
						setState(416);
=======
						setState(382);
						match(T__7);
						setState(383);
						match(IDENTIFIER);
						}
						}
						setState(388);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

<<<<<<< HEAD
				setState(419);
				match(T__2);
				setState(420);
				match(T__4);
				setState(421);
=======
				setState(391);
				match(T__2);
				setState(392);
				match(T__4);
				setState(393);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
				expression(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
<<<<<<< HEAD
		case 17:
			return type_sempred((TypeContext)_localctx, predIndex);
		case 20:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		case 32:
=======
		case 14:
			return type_sempred((TypeContext)_localctx, predIndex);
		case 17:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		case 28:
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
			return pattern_sempred((PatternContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean type_sempred(TypeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
<<<<<<< HEAD
			return precpred(_ctx, 10);
		case 2:
			return precpred(_ctx, 9);
		case 3:
			return precpred(_ctx, 8);
		case 4:
			return precpred(_ctx, 7);
		case 5:
			return precpred(_ctx, 6);
		case 6:
			return precpred(_ctx, 5);
		case 7:
			return precpred(_ctx, 15);
		case 8:
			return precpred(_ctx, 14);
		case 9:
			return precpred(_ctx, 13);
=======
			return precpred(_ctx, 8);
		case 2:
			return precpred(_ctx, 7);
		case 3:
			return precpred(_ctx, 6);
		case 4:
			return precpred(_ctx, 5);
		case 5:
			return precpred(_ctx, 4);
		case 6:
			return precpred(_ctx, 3);
		case 7:
			return precpred(_ctx, 13);
		case 8:
			return precpred(_ctx, 12);
		case 9:
			return precpred(_ctx, 11);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		}
		return true;
	}
	private boolean pattern_sempred(PatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 10:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
<<<<<<< HEAD
		"\u0004\u00017\u01a9\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
=======
		"\u0004\u00013\u018d\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
<<<<<<< HEAD
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0001"+
		"\u0000\u0005\u0000H\b\u0000\n\u0000\f\u0000K\t\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001S\b"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002X\b\u0002\u0001"+
		"\u0002\u0001\u0002\u0003\u0002\\\b\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u0002a\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0005\u0003i\b\u0003\n\u0003\f\u0003l\t"+
		"\u0003\u0001\u0003\u0003\u0003o\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0005\u0004t\b\u0004\n\u0004\f\u0004w\t\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005|\b\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0003\u0006\u0081\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0089\b\u0007\n\u0007\f\u0007"+
		"\u008c\t\u0007\u0001\b\u0001\b\u0003\b\u0090\b\b\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0003\t\u0097\b\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u00a1\b\u000b\n\u000b"+
		"\f\u000b\u00a4\t\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r"+
		"\u0001\r\u0005\r\u00ad\b\r\n\r\f\r\u00b0\t\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0003\u000e\u00b6\b\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u00bf"+
		"\b\u000f\n\u000f\f\u000f\u00c2\t\u000f\u0001\u000f\u0001\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0005\u0011\u00d4\b\u0011\n\u0011\f\u0011\u00d7\t\u0011\u0001"+
		"\u0011\u0001\u0011\u0003\u0011\u00db\b\u0011\u0001\u0011\u0001\u0011\u0005"+
		"\u0011\u00df\b\u0011\n\u0011\f\u0011\u00e2\t\u0011\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0005\u0012\u00e7\b\u0012\n\u0012\f\u0012\u00ea\t\u0012\u0001"+
		"\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003"+
		"\u0014\u00f8\b\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0003\u0014\u0112\b\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0005\u0014\u011a\b\u0014\n"+
		"\u0014\f\u0014\u011d\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0127"+
		"\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0005\u0017\u0130\b\u0017\n\u0017\f\u0017\u0133\t\u0017"+
		"\u0001\u0017\u0003\u0017\u0136\b\u0017\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u013e\b\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0004\u0019\u0144\b\u0019\u000b\u0019"+
		"\f\u0019\u0145\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0003\u001a\u014d\b\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b"+
		"\u0001\u001b\u0005\u001b\u0154\b\u001b\n\u001b\f\u001b\u0157\t\u001b\u0001"+
		"\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001c\u0003\u001c\u0160\b\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001"+
		"\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0004\u001e\u016e\b\u001e\u000b\u001e\f"+
		"\u001e\u016f\u0001\u001e\u0001\u001e\u0001\u001f\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u017e"+
		"\b \u0001 \u0001 \u0001 \u0005 \u0183\b \n \f \u0186\t \u0001!\u0001!"+
		"\u0001!\u0001!\u0001!\u0005!\u018d\b!\n!\f!\u0190\t!\u0003!\u0192\b!\u0001"+
		"!\u0001!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0005"+
		"\"\u019d\b\"\n\"\f\"\u01a0\t\"\u0003\"\u01a2\b\"\u0001\"\u0001\"\u0001"+
		"\"\u0003\"\u01a7\b\"\u0001\"\u0000\u0003\"(@#\u0000\u0002\u0004\u0006"+
		"\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,."+
		"02468:<>@BD\u0000\u0004\u0001\u0000\u0010\u0016\u0001\u0000\u001c\u001e"+
		"\u0002\u0000\u001a\u001a\u001f\u001f\u0002\u0000\r\u000e #\u01c7\u0000"+
		"I\u0001\u0000\u0000\u0000\u0002R\u0001\u0000\u0000\u0000\u0004T\u0001"+
		"\u0000\u0000\u0000\u0006n\u0001\u0000\u0000\u0000\bp\u0001\u0000\u0000"+
		"\u0000\nx\u0001\u0000\u0000\u0000\f}\u0001\u0000\u0000\u0000\u000e\u0085"+
		"\u0001\u0000\u0000\u0000\u0010\u008f\u0001\u0000\u0000\u0000\u0012\u0091"+
		"\u0001\u0000\u0000\u0000\u0014\u0098\u0001\u0000\u0000\u0000\u0016\u009d"+
		"\u0001\u0000\u0000\u0000\u0018\u00a5\u0001\u0000\u0000\u0000\u001a\u00a9"+
		"\u0001\u0000\u0000\u0000\u001c\u00b1\u0001\u0000\u0000\u0000\u001e\u00ba"+
		"\u0001\u0000\u0000\u0000 \u00c5\u0001\u0000\u0000\u0000\"\u00da\u0001"+
		"\u0000\u0000\u0000$\u00e3\u0001\u0000\u0000\u0000&\u00eb\u0001\u0000\u0000"+
		"\u0000(\u00f7\u0001\u0000\u0000\u0000*\u0126\u0001\u0000\u0000\u0000,"+
		"\u0128\u0001\u0000\u0000\u0000.\u0131\u0001\u0000\u0000\u00000\u013d\u0001"+
		"\u0000\u0000\u00002\u013f\u0001\u0000\u0000\u00004\u0149\u0001\u0000\u0000"+
		"\u00006\u0150\u0001\u0000\u0000\u00008\u0158\u0001\u0000\u0000\u0000:"+
		"\u0161\u0001\u0000\u0000\u0000<\u0169\u0001\u0000\u0000\u0000>\u0173\u0001"+
		"\u0000\u0000\u0000@\u017d\u0001\u0000\u0000\u0000B\u0187\u0001\u0000\u0000"+
		"\u0000D\u01a6\u0001\u0000\u0000\u0000FH\u0003\u0002\u0001\u0000GF\u0001"+
		"\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000"+
		"IJ\u0001\u0000\u0000\u0000JL\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000"+
		"\u0000LM\u0005\u0000\u0000\u0001M\u0001\u0001\u0000\u0000\u0000NS\u0003"+
		"\u0004\u0002\u0000OS\u0003\f\u0006\u0000PS\u0003\u001c\u000e\u0000QS\u0003"+
		"(\u0014\u0000RN\u0001\u0000\u0000\u0000RO\u0001\u0000\u0000\u0000RP\u0001"+
		"\u0000\u0000\u0000RQ\u0001\u0000\u0000\u0000S\u0003\u0001\u0000\u0000"+
		"\u0000TU\u0005\u0001\u0000\u0000UW\u00054\u0000\u0000VX\u0003\u001e\u000f"+
		"\u0000WV\u0001\u0000\u0000\u0000WX\u0001\u0000\u0000\u0000XY\u0001\u0000"+
		"\u0000\u0000Y[\u0005\u0002\u0000\u0000Z\\\u0003\b\u0004\u0000[Z\u0001"+
		"\u0000\u0000\u0000[\\\u0001\u0000\u0000\u0000\\]\u0001\u0000\u0000\u0000"+
		"]`\u0005\u0003\u0000\u0000^_\u0005\u0004\u0000\u0000_a\u0003\"\u0011\u0000"+
		"`^\u0001\u0000\u0000\u0000`a\u0001\u0000\u0000\u0000ab\u0001\u0000\u0000"+
		"\u0000bc\u0005\u0005\u0000\u0000cd\u0003\u0006\u0003\u0000d\u0005\u0001"+
		"\u0000\u0000\u0000eo\u0003(\u0014\u0000fj\u0005\u0006\u0000\u0000gi\u0003"+
		"\u0002\u0001\u0000hg\u0001\u0000\u0000\u0000il\u0001\u0000\u0000\u0000"+
		"jh\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000km\u0001\u0000\u0000"+
		"\u0000lj\u0001\u0000\u0000\u0000mo\u0005\u0007\u0000\u0000ne\u0001\u0000"+
		"\u0000\u0000nf\u0001\u0000\u0000\u0000o\u0007\u0001\u0000\u0000\u0000"+
		"pu\u0003\n\u0005\u0000qr\u0005\b\u0000\u0000rt\u0003\n\u0005\u0000sq\u0001"+
		"\u0000\u0000\u0000tw\u0001\u0000\u0000\u0000us\u0001\u0000\u0000\u0000"+
		"uv\u0001\u0000\u0000\u0000v\t\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000"+
		"\u0000x{\u00054\u0000\u0000yz\u0005\u0004\u0000\u0000z|\u0003\"\u0011"+
		"\u0000{y\u0001\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000|\u000b\u0001"+
		"\u0000\u0000\u0000}~\u0005\t\u0000\u0000~\u0080\u00054\u0000\u0000\u007f"+
		"\u0081\u0003\u001e\u000f\u0000\u0080\u007f\u0001\u0000\u0000\u0000\u0080"+
		"\u0081\u0001\u0000\u0000\u0000\u0081\u0082\u0001\u0000\u0000\u0000\u0082"+
		"\u0083\u0005\n\u0000\u0000\u0083\u0084\u0003\u000e\u0007\u0000\u0084\r"+
		"\u0001\u0000\u0000\u0000\u0085\u008a\u0003\u0010\b\u0000\u0086\u0087\u0005"+
		"\u000b\u0000\u0000\u0087\u0089\u0003\u0010\b\u0000\u0088\u0086\u0001\u0000"+
		"\u0000\u0000\u0089\u008c\u0001\u0000\u0000\u0000\u008a\u0088\u0001\u0000"+
		"\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b\u000f\u0001\u0000"+
		"\u0000\u0000\u008c\u008a\u0001\u0000\u0000\u0000\u008d\u0090\u0003\u0012"+
		"\t\u0000\u008e\u0090\u0003\u0014\n\u0000\u008f\u008d\u0001\u0000\u0000"+
		"\u0000\u008f\u008e\u0001\u0000\u0000\u0000\u0090\u0011\u0001\u0000\u0000"+
		"\u0000\u0091\u0096\u00054\u0000\u0000\u0092\u0093\u0005\u0002\u0000\u0000"+
		"\u0093\u0094\u0003\u001a\r\u0000\u0094\u0095\u0005\u0003\u0000\u0000\u0095"+
		"\u0097\u0001\u0000\u0000\u0000\u0096\u0092\u0001\u0000\u0000\u0000\u0096"+
		"\u0097\u0001\u0000\u0000\u0000\u0097\u0013\u0001\u0000\u0000\u0000\u0098"+
		"\u0099\u00054\u0000\u0000\u0099\u009a\u0005\u0002\u0000\u0000\u009a\u009b"+
		"\u0003\u0016\u000b\u0000\u009b\u009c\u0005\u0003\u0000\u0000\u009c\u0015"+
		"\u0001\u0000\u0000\u0000\u009d\u00a2\u0003\u0018\f\u0000\u009e\u009f\u0005"+
		"\b\u0000\u0000\u009f\u00a1\u0003\u0018\f\u0000\u00a0\u009e\u0001\u0000"+
		"\u0000\u0000\u00a1\u00a4\u0001\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000\u00a3\u0017\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a5\u00a6\u00054\u0000"+
		"\u0000\u00a6\u00a7\u0005\u0004\u0000\u0000\u00a7\u00a8\u0003\"\u0011\u0000"+
		"\u00a8\u0019\u0001\u0000\u0000\u0000\u00a9\u00ae\u0003\"\u0011\u0000\u00aa"+
		"\u00ab\u0005\b\u0000\u0000\u00ab\u00ad\u0003\"\u0011\u0000\u00ac\u00aa"+
		"\u0001\u0000\u0000\u0000\u00ad\u00b0\u0001\u0000\u0000\u0000\u00ae\u00ac"+
		"\u0001\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u001b"+
		"\u0001\u0000\u0000\u0000\u00b0\u00ae\u0001\u0000\u0000\u0000\u00b1\u00b2"+
		"\u0005\f\u0000\u0000\u00b2\u00b5\u00054\u0000\u0000\u00b3\u00b4\u0005"+
		"\u0004\u0000\u0000\u00b4\u00b6\u0003\"\u0011\u0000\u00b5\u00b3\u0001\u0000"+
		"\u0000\u0000\u00b5\u00b6\u0001\u0000\u0000\u0000\u00b6\u00b7\u0001\u0000"+
		"\u0000\u0000\u00b7\u00b8\u0005\n\u0000\u0000\u00b8\u00b9\u0003(\u0014"+
		"\u0000\u00b9\u001d\u0001\u0000\u0000\u0000\u00ba\u00bb\u0005\r\u0000\u0000"+
		"\u00bb\u00c0\u0003 \u0010\u0000\u00bc\u00bd\u0005\b\u0000\u0000\u00bd"+
		"\u00bf\u0003 \u0010\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00bf\u00c2"+
		"\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000\u0000\u0000\u00c0\u00c1"+
		"\u0001\u0000\u0000\u0000\u00c1\u00c3\u0001\u0000\u0000\u0000\u00c2\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c3\u00c4\u0005\u000e\u0000\u0000\u00c4\u001f"+
		"\u0001\u0000\u0000\u0000\u00c5\u00c6\u00054\u0000\u0000\u00c6!\u0001\u0000"+
		"\u0000\u0000\u00c7\u00c8\u0006\u0011\uffff\uffff\u0000\u00c8\u00db\u0003"+
		"&\u0013\u0000\u00c9\u00db\u00054\u0000\u0000\u00ca\u00cb\u00054\u0000"+
		"\u0000\u00cb\u00cc\u0005\r\u0000\u0000\u00cc\u00cd\u0003$\u0012\u0000"+
		"\u00cd\u00ce\u0005\u000e\u0000\u0000\u00ce\u00db\u0001\u0000\u0000\u0000"+
		"\u00cf\u00d0\u0005\u0002\u0000\u0000\u00d0\u00d5\u0003\"\u0011\u0000\u00d1"+
		"\u00d2\u0005\b\u0000\u0000\u00d2\u00d4\u0003\"\u0011\u0000\u00d3\u00d1"+
		"\u0001\u0000\u0000\u0000\u00d4\u00d7\u0001\u0000\u0000\u0000\u00d5\u00d3"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001\u0000\u0000\u0000\u00d6\u00d8"+
		"\u0001\u0000\u0000\u0000\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d8\u00d9"+
		"\u0005\u0003\u0000\u0000\u00d9\u00db\u0001\u0000\u0000\u0000\u00da\u00c7"+
		"\u0001\u0000\u0000\u0000\u00da\u00c9\u0001\u0000\u0000\u0000\u00da\u00ca"+
		"\u0001\u0000\u0000\u0000\u00da\u00cf\u0001\u0000\u0000\u0000\u00db\u00e0"+
		"\u0001\u0000\u0000\u0000\u00dc\u00dd\n\u0002\u0000\u0000\u00dd\u00df\u0005"+
		"\u000f\u0000\u0000\u00de\u00dc\u0001\u0000\u0000\u0000\u00df\u00e2\u0001"+
		"\u0000\u0000\u0000\u00e0\u00de\u0001\u0000\u0000\u0000\u00e0\u00e1\u0001"+
		"\u0000\u0000\u0000\u00e1#\u0001\u0000\u0000\u0000\u00e2\u00e0\u0001\u0000"+
		"\u0000\u0000\u00e3\u00e8\u0003\"\u0011\u0000\u00e4\u00e5\u0005\b\u0000"+
		"\u0000\u00e5\u00e7\u0003\"\u0011\u0000\u00e6\u00e4\u0001\u0000\u0000\u0000"+
		"\u00e7\u00ea\u0001\u0000\u0000\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000"+
		"\u00e8\u00e9\u0001\u0000\u0000\u0000\u00e9%\u0001\u0000\u0000\u0000\u00ea"+
		"\u00e8\u0001\u0000\u0000\u0000\u00eb\u00ec\u0007\u0000\u0000\u0000\u00ec"+
		"\'\u0001\u0000\u0000\u0000\u00ed\u00ee\u0006\u0014\uffff\uffff\u0000\u00ee"+
		"\u00f8\u0003*\u0015\u0000\u00ef\u00f0\u0005\u001a\u0000\u0000\u00f0\u00f8"+
		"\u0003(\u0014\f\u00f1\u00f2\u0005\u001b\u0000\u0000\u00f2\u00f8\u0003"+
		"(\u0014\u000b\u00f3\u00f8\u00038\u001c\u0000\u00f4\u00f8\u0003:\u001d"+
		"\u0000\u00f5\u00f8\u0003<\u001e\u0000\u00f6\u00f8\u0003D\"\u0000\u00f7"+
		"\u00ed\u0001\u0000\u0000\u0000\u00f7\u00ef\u0001\u0000\u0000\u0000\u00f7"+
		"\u00f1\u0001\u0000\u0000\u0000\u00f7\u00f3\u0001\u0000\u0000\u0000\u00f7"+
		"\u00f4\u0001\u0000\u0000\u0000\u00f7\u00f5\u0001\u0000\u0000\u0000\u00f7"+
		"\u00f6\u0001\u0000\u0000\u0000\u00f8\u011b\u0001\u0000\u0000\u0000\u00f9"+
		"\u00fa\n\n\u0000\u0000\u00fa\u00fb\u0007\u0001\u0000\u0000\u00fb\u011a"+
		"\u0003(\u0014\u000b\u00fc\u00fd\n\t\u0000\u0000\u00fd\u00fe\u0007\u0002"+
		"\u0000\u0000\u00fe\u011a\u0003(\u0014\n\u00ff\u0100\n\b\u0000\u0000\u0100"+
		"\u0101\u0007\u0003\u0000\u0000\u0101\u011a\u0003(\u0014\t\u0102\u0103"+
		"\n\u0007\u0000\u0000\u0103\u0104\u0005$\u0000\u0000\u0104\u011a\u0003"+
		"(\u0014\b\u0105\u0106\n\u0006\u0000\u0000\u0106\u0107\u0005%\u0000\u0000"+
		"\u0107\u011a\u0003(\u0014\u0007\u0108\u0109\n\u0005\u0000\u0000\u0109"+
		"\u010a\u0005&\u0000\u0000\u010a\u011a\u0003(\u0014\u0006\u010b\u010c\n"+
		"\u000f\u0000\u0000\u010c\u010d\u0005\u0017\u0000\u0000\u010d\u011a\u0005"+
		"4\u0000\u0000\u010e\u010f\n\u000e\u0000\u0000\u010f\u0111\u0005\u0002"+
		"\u0000\u0000\u0110\u0112\u00036\u001b\u0000\u0111\u0110\u0001\u0000\u0000"+
		"\u0000\u0111\u0112\u0001\u0000\u0000\u0000\u0112\u0113\u0001\u0000\u0000"+
		"\u0000\u0113\u011a\u0005\u0003\u0000\u0000\u0114\u0115\n\r\u0000\u0000"+
		"\u0115\u0116\u0005\u0018\u0000\u0000\u0116\u0117\u0003(\u0014\u0000\u0117"+
		"\u0118\u0005\u0019\u0000\u0000\u0118\u011a\u0001\u0000\u0000\u0000\u0119"+
		"\u00f9\u0001\u0000\u0000\u0000\u0119\u00fc\u0001\u0000\u0000\u0000\u0119"+
		"\u00ff\u0001\u0000\u0000\u0000\u0119\u0102\u0001\u0000\u0000\u0000\u0119"+
		"\u0105\u0001\u0000\u0000\u0000\u0119\u0108\u0001\u0000\u0000\u0000\u0119"+
		"\u010b\u0001\u0000\u0000\u0000\u0119\u010e\u0001\u0000\u0000\u0000\u0119"+
		"\u0114\u0001\u0000\u0000\u0000\u011a\u011d\u0001\u0000\u0000\u0000\u011b"+
		"\u0119\u0001\u0000\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c"+
		")\u0001\u0000\u0000\u0000\u011d\u011b\u0001\u0000\u0000\u0000\u011e\u0127"+
		"\u00054\u0000\u0000\u011f\u0127\u00030\u0018\u0000\u0120\u0121\u0005\u0002"+
		"\u0000\u0000\u0121\u0122\u0003(\u0014\u0000\u0122\u0123\u0005\u0003\u0000"+
		"\u0000\u0123\u0127\u0001\u0000\u0000\u0000\u0124\u0127\u00034\u001a\u0000"+
		"\u0125\u0127\u0003,\u0016\u0000\u0126\u011e\u0001\u0000\u0000\u0000\u0126"+
		"\u011f\u0001\u0000\u0000\u0000\u0126\u0120\u0001\u0000\u0000\u0000\u0126"+
		"\u0124\u0001\u0000\u0000\u0000\u0126\u0125\u0001\u0000\u0000\u0000\u0127"+
		"+\u0001\u0000\u0000\u0000\u0128\u0129\u0005\u0006\u0000\u0000\u0129\u012a"+
		"\u0003.\u0017\u0000\u012a\u012b\u0005\u0007\u0000\u0000\u012b-\u0001\u0000"+
		"\u0000\u0000\u012c\u012d\u0003\u0002\u0001\u0000\u012d\u012e\u0005\'\u0000"+
		"\u0000\u012e\u0130\u0001\u0000\u0000\u0000\u012f\u012c\u0001\u0000\u0000"+
		"\u0000\u0130\u0133\u0001\u0000\u0000\u0000\u0131\u012f\u0001\u0000\u0000"+
		"\u0000\u0131\u0132\u0001\u0000\u0000\u0000\u0132\u0135\u0001\u0000\u0000"+
		"\u0000\u0133\u0131\u0001\u0000\u0000\u0000\u0134\u0136\u0003(\u0014\u0000"+
		"\u0135\u0134\u0001\u0000\u0000\u0000\u0135\u0136\u0001\u0000\u0000\u0000"+
		"\u0136/\u0001\u0000\u0000\u0000\u0137\u013e\u00051\u0000\u0000\u0138\u013e"+
		"\u00052\u0000\u0000\u0139\u013e\u00053\u0000\u0000\u013a\u013e\u00050"+
		"\u0000\u0000\u013b\u013e\u00032\u0019\u0000\u013c\u013e\u0005(\u0000\u0000"+
		"\u013d\u0137\u0001\u0000\u0000\u0000\u013d\u0138\u0001\u0000\u0000\u0000"+
		"\u013d\u0139\u0001\u0000\u0000\u0000\u013d\u013a\u0001\u0000\u0000\u0000"+
		"\u013d\u013b\u0001\u0000\u0000\u0000\u013d\u013c\u0001\u0000\u0000\u0000"+
		"\u013e1\u0001\u0000\u0000\u0000\u013f\u0140\u0005\u0002\u0000\u0000\u0140"+
		"\u0143\u0003(\u0014\u0000\u0141\u0142\u0005\b\u0000\u0000\u0142\u0144"+
		"\u0003(\u0014\u0000\u0143\u0141\u0001\u0000\u0000\u0000\u0144\u0145\u0001"+
		"\u0000\u0000\u0000\u0145\u0143\u0001\u0000\u0000\u0000\u0145\u0146\u0001"+
		"\u0000\u0000\u0000\u0146\u0147\u0001\u0000\u0000\u0000\u0147\u0148\u0005"+
		"\u0003\u0000\u0000\u01483\u0001\u0000\u0000\u0000\u0149\u014a\u00054\u0000"+
		"\u0000\u014a\u014c\u0005\u0002\u0000\u0000\u014b\u014d\u00036\u001b\u0000"+
		"\u014c\u014b\u0001\u0000\u0000\u0000\u014c\u014d\u0001\u0000\u0000\u0000"+
		"\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u014f\u0005\u0003\u0000\u0000"+
		"\u014f5\u0001\u0000\u0000\u0000\u0150\u0155\u0003(\u0014\u0000\u0151\u0152"+
		"\u0005\b\u0000\u0000\u0152\u0154\u0003(\u0014\u0000\u0153\u0151\u0001"+
		"\u0000\u0000\u0000\u0154\u0157\u0001\u0000\u0000\u0000\u0155\u0153\u0001"+
		"\u0000\u0000\u0000\u0155\u0156\u0001\u0000\u0000\u0000\u01567\u0001\u0000"+
		"\u0000\u0000\u0157\u0155\u0001\u0000\u0000\u0000\u0158\u0159\u0005)\u0000"+
		"\u0000\u0159\u015a\u0005\u0002\u0000\u0000\u015a\u015b\u0003(\u0014\u0000"+
		"\u015b\u015c\u0005\u0003\u0000\u0000\u015c\u015f\u0003(\u0014\u0000\u015d"+
		"\u015e\u0005*\u0000\u0000\u015e\u0160\u0003(\u0014\u0000\u015f\u015d\u0001"+
		"\u0000\u0000\u0000\u015f\u0160\u0001\u0000\u0000\u0000\u01609\u0001\u0000"+
		"\u0000\u0000\u0161\u0162\u0005+\u0000\u0000\u0162\u0163\u0005\u0002\u0000"+
		"\u0000\u0163\u0164\u00054\u0000\u0000\u0164\u0165\u0005,\u0000\u0000\u0165"+
		"\u0166\u0003(\u0014\u0000\u0166\u0167\u0005\u0003\u0000\u0000\u0167\u0168"+
		"\u0003(\u0014\u0000\u0168;\u0001\u0000\u0000\u0000\u0169\u016a\u0005-"+
		"\u0000\u0000\u016a\u016b\u0003(\u0014\u0000\u016b\u016d\u0005\u0006\u0000"+
		"\u0000\u016c\u016e\u0003>\u001f\u0000\u016d\u016c\u0001\u0000\u0000\u0000"+
		"\u016e\u016f\u0001\u0000\u0000\u0000\u016f\u016d\u0001\u0000\u0000\u0000"+
		"\u016f\u0170\u0001\u0000\u0000\u0000\u0170\u0171\u0001\u0000\u0000\u0000"+
		"\u0171\u0172\u0005\u0007\u0000\u0000\u0172=\u0001\u0000\u0000\u0000\u0173"+
		"\u0174\u0005.\u0000\u0000\u0174\u0175\u0003@ \u0000\u0175\u0176\u0005"+
		"\u0005\u0000\u0000\u0176\u0177\u0003(\u0014\u0000\u0177?\u0001\u0000\u0000"+
		"\u0000\u0178\u0179\u0006 \uffff\uffff\u0000\u0179\u017e\u0005/\u0000\u0000"+
		"\u017a\u017e\u00054\u0000\u0000\u017b\u017e\u00030\u0018\u0000\u017c\u017e"+
		"\u0003B!\u0000\u017d\u0178\u0001\u0000\u0000\u0000\u017d\u017a\u0001\u0000"+
		"\u0000\u0000\u017d\u017b\u0001\u0000\u0000\u0000\u017d\u017c\u0001\u0000"+
		"\u0000\u0000\u017e\u0184\u0001\u0000\u0000\u0000\u017f\u0180\n\u0001\u0000"+
		"\u0000\u0180\u0181\u0005)\u0000\u0000\u0181\u0183\u0003(\u0014\u0000\u0182"+
		"\u017f\u0001\u0000\u0000\u0000\u0183\u0186\u0001\u0000\u0000\u0000\u0184"+
		"\u0182\u0001\u0000\u0000\u0000\u0184\u0185\u0001\u0000\u0000\u0000\u0185"+
		"A\u0001\u0000\u0000\u0000\u0186\u0184\u0001\u0000\u0000\u0000\u0187\u0188"+
		"\u00054\u0000\u0000\u0188\u0191\u0005\u0002\u0000\u0000\u0189\u018e\u0003"+
		"@ \u0000\u018a\u018b\u0005\b\u0000\u0000\u018b\u018d\u0003@ \u0000\u018c"+
		"\u018a\u0001\u0000\u0000\u0000\u018d\u0190\u0001\u0000\u0000\u0000\u018e"+
		"\u018c\u0001\u0000\u0000\u0000\u018e\u018f\u0001\u0000\u0000\u0000\u018f"+
		"\u0192\u0001\u0000\u0000\u0000\u0190\u018e\u0001\u0000\u0000\u0000\u0191"+
		"\u0189\u0001\u0000\u0000\u0000\u0191\u0192\u0001\u0000\u0000\u0000\u0192"+
		"\u0193\u0001\u0000\u0000\u0000\u0193\u0194\u0005\u0003\u0000\u0000\u0194"+
		"C\u0001\u0000\u0000\u0000\u0195\u0196\u00054\u0000\u0000\u0196\u0197\u0005"+
		"\u0005\u0000\u0000\u0197\u01a7\u0003(\u0014\u0000\u0198\u01a1\u0005\u0002"+
		"\u0000\u0000\u0199\u019e\u00054\u0000\u0000\u019a\u019b\u0005\b\u0000"+
		"\u0000\u019b\u019d\u00054\u0000\u0000\u019c\u019a\u0001\u0000\u0000\u0000"+
		"\u019d\u01a0\u0001\u0000\u0000\u0000\u019e\u019c\u0001\u0000\u0000\u0000"+
		"\u019e\u019f\u0001\u0000\u0000\u0000\u019f\u01a2\u0001\u0000\u0000\u0000"+
		"\u01a0\u019e\u0001\u0000\u0000\u0000\u01a1\u0199\u0001\u0000\u0000\u0000"+
		"\u01a1\u01a2\u0001\u0000\u0000\u0000\u01a2\u01a3\u0001\u0000\u0000\u0000"+
		"\u01a3\u01a4\u0005\u0003\u0000\u0000\u01a4\u01a5\u0005\u0005\u0000\u0000"+
		"\u01a5\u01a7\u0003(\u0014\u0000\u01a6\u0195\u0001\u0000\u0000\u0000\u01a6"+
		"\u0198\u0001\u0000\u0000\u0000\u01a7E\u0001\u0000\u0000\u0000)IRW[`jn"+
		"u{\u0080\u008a\u008f\u0096\u00a2\u00ae\u00b5\u00c0\u00d5\u00da\u00e0\u00e8"+
		"\u00f7\u0111\u0119\u011b\u0126\u0131\u0135\u013d\u0145\u014c\u0155\u015f"+
		"\u016f\u017d\u0184\u018e\u0191\u019e\u01a1\u01a6";
=======
		"\u0001\u0000\u0005\u0000@\b\u0000\n\u0000\f\u0000C\t\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001"+
		"K\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002P\b\u0002\u0001"+
		"\u0002\u0001\u0002\u0003\u0002T\b\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u0002Y\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0005\u0003a\b\u0003\n\u0003\f\u0003d\t"+
		"\u0003\u0001\u0003\u0003\u0003g\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0005\u0004l\b\u0004\n\u0004\f\u0004o\t\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0003\u0005t\b\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0003\u0006y\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0081\b\u0007\n\u0007\f\u0007"+
		"\u0084\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u008b\b"+
		"\b\u0001\t\u0001\t\u0001\t\u0005\t\u0090\b\t\n\t\f\t\u0093\t\t\u0001\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0003\u000b\u009d\b\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f"+
		"\u0001\f\u0001\f\u0001\f\u0005\f\u00a6\b\f\n\f\f\f\u00a9\t\f\u0001\f\u0001"+
		"\f\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0005\u000e\u00bb\b\u000e\n\u000e\f\u000e\u00be\t\u000e"+
		"\u0001\u000e\u0001\u000e\u0003\u000e\u00c2\b\u000e\u0001\u000e\u0001\u000e"+
		"\u0005\u000e\u00c6\b\u000e\n\u000e\f\u000e\u00c9\t\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0005\u000f\u00ce\b\u000f\n\u000f\f\u000f\u00d1\t\u000f"+
		"\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u00dd\b\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0003\u0011\u00f7\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0005\u0011\u00ff\b\u0011\n\u0011\f\u0011\u0102"+
		"\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0003\u0012\u010b\b\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003"+
		"\u0013\u0115\b\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0005"+
		"\u0014\u011b\b\u0014\n\u0014\f\u0014\u011e\t\u0014\u0003\u0014\u0120\b"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0005\u0015\u0128\b\u0015\n\u0015\f\u0015\u012b\t\u0015\u0003\u0015"+
		"\u012d\b\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0004\u0017"+
		"\u0139\b\u0017\u000b\u0017\f\u0017\u013a\u0001\u0017\u0001\u0017\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u0142\b\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0005\u0019\u0149\b\u0019\n"+
		"\u0019\f\u0019\u014c\t\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001"+
		"\u001a\u0004\u001a\u0152\b\u001a\u000b\u001a\f\u001a\u0153\u0001\u001a"+
		"\u0001\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0003\u001c"+
		"\u0162\b\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c\u0167\b"+
		"\u001c\n\u001c\f\u001c\u016a\t\u001c\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0005\u001d\u0171\b\u001d\n\u001d\f\u001d\u0174"+
		"\t\u001d\u0003\u001d\u0176\b\u001d\u0001\u001d\u0001\u001d\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0005\u001e\u0181\b\u001e\n\u001e\f\u001e\u0184\t\u001e\u0003\u001e\u0186"+
		"\b\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u018b\b\u001e"+
		"\u0001\u001e\u0000\u0003\u001c\"8\u001f\u0000\u0002\u0004\u0006\b\n\f"+
		"\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:"+
		"<\u0000\u0004\u0001\u0000\u0010\u0016\u0001\u0000\u001c\u001e\u0002\u0000"+
		"\u001a\u001a\u001f\u001f\u0002\u0000\r\u000e #\u01ad\u0000A\u0001\u0000"+
		"\u0000\u0000\u0002J\u0001\u0000\u0000\u0000\u0004L\u0001\u0000\u0000\u0000"+
		"\u0006f\u0001\u0000\u0000\u0000\bh\u0001\u0000\u0000\u0000\np\u0001\u0000"+
		"\u0000\u0000\fu\u0001\u0000\u0000\u0000\u000e}\u0001\u0000\u0000\u0000"+
		"\u0010\u0085\u0001\u0000\u0000\u0000\u0012\u008c\u0001\u0000\u0000\u0000"+
		"\u0014\u0094\u0001\u0000\u0000\u0000\u0016\u0098\u0001\u0000\u0000\u0000"+
		"\u0018\u00a1\u0001\u0000\u0000\u0000\u001a\u00ac\u0001\u0000\u0000\u0000"+
		"\u001c\u00c1\u0001\u0000\u0000\u0000\u001e\u00ca\u0001\u0000\u0000\u0000"+
		" \u00d2\u0001\u0000\u0000\u0000\"\u00dc\u0001\u0000\u0000\u0000$\u010a"+
		"\u0001\u0000\u0000\u0000&\u0114\u0001\u0000\u0000\u0000(\u0116\u0001\u0000"+
		"\u0000\u0000*\u0123\u0001\u0000\u0000\u0000,\u0130\u0001\u0000\u0000\u0000"+
		".\u0134\u0001\u0000\u0000\u00000\u013e\u0001\u0000\u0000\u00002\u0145"+
		"\u0001\u0000\u0000\u00004\u014d\u0001\u0000\u0000\u00006\u0157\u0001\u0000"+
		"\u0000\u00008\u0161\u0001\u0000\u0000\u0000:\u016b\u0001\u0000\u0000\u0000"+
		"<\u018a\u0001\u0000\u0000\u0000>@\u0003\u0002\u0001\u0000?>\u0001\u0000"+
		"\u0000\u0000@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001"+
		"\u0000\u0000\u0000BD\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000"+
		"DE\u0005\u0000\u0000\u0001E\u0001\u0001\u0000\u0000\u0000FK\u0003\u0004"+
		"\u0002\u0000GK\u0003\f\u0006\u0000HK\u0003\u0016\u000b\u0000IK\u0003\""+
		"\u0011\u0000JF\u0001\u0000\u0000\u0000JG\u0001\u0000\u0000\u0000JH\u0001"+
		"\u0000\u0000\u0000JI\u0001\u0000\u0000\u0000K\u0003\u0001\u0000\u0000"+
		"\u0000LM\u0005\u0001\u0000\u0000MO\u0005,\u0000\u0000NP\u0003\u0018\f"+
		"\u0000ON\u0001\u0000\u0000\u0000OP\u0001\u0000\u0000\u0000PQ\u0001\u0000"+
		"\u0000\u0000QS\u0005\u0002\u0000\u0000RT\u0003\b\u0004\u0000SR\u0001\u0000"+
		"\u0000\u0000ST\u0001\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000UX\u0005"+
		"\u0003\u0000\u0000VW\u0005\u0004\u0000\u0000WY\u0003\u001c\u000e\u0000"+
		"XV\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000YZ\u0001\u0000\u0000"+
		"\u0000Z[\u0005\u0005\u0000\u0000[\\\u0003\u0006\u0003\u0000\\\u0005\u0001"+
		"\u0000\u0000\u0000]g\u0003\"\u0011\u0000^b\u0005\u0006\u0000\u0000_a\u0003"+
		"\u0002\u0001\u0000`_\u0001\u0000\u0000\u0000ad\u0001\u0000\u0000\u0000"+
		"b`\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ce\u0001\u0000\u0000"+
		"\u0000db\u0001\u0000\u0000\u0000eg\u0005\u0007\u0000\u0000f]\u0001\u0000"+
		"\u0000\u0000f^\u0001\u0000\u0000\u0000g\u0007\u0001\u0000\u0000\u0000"+
		"hm\u0003\n\u0005\u0000ij\u0005\b\u0000\u0000jl\u0003\n\u0005\u0000ki\u0001"+
		"\u0000\u0000\u0000lo\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000"+
		"mn\u0001\u0000\u0000\u0000n\t\u0001\u0000\u0000\u0000om\u0001\u0000\u0000"+
		"\u0000ps\u0005,\u0000\u0000qr\u0005\u0004\u0000\u0000rt\u0003\u001c\u000e"+
		"\u0000sq\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000t\u000b\u0001"+
		"\u0000\u0000\u0000uv\u0005\t\u0000\u0000vx\u0005,\u0000\u0000wy\u0003"+
		"\u0018\f\u0000xw\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000\u0000yz\u0001"+
		"\u0000\u0000\u0000z{\u0005\n\u0000\u0000{|\u0003\u000e\u0007\u0000|\r"+
		"\u0001\u0000\u0000\u0000}\u0082\u0003\u0010\b\u0000~\u007f\u0005\u000b"+
		"\u0000\u0000\u007f\u0081\u0003\u0010\b\u0000\u0080~\u0001\u0000\u0000"+
		"\u0000\u0081\u0084\u0001\u0000\u0000\u0000\u0082\u0080\u0001\u0000\u0000"+
		"\u0000\u0082\u0083\u0001\u0000\u0000\u0000\u0083\u000f\u0001\u0000\u0000"+
		"\u0000\u0084\u0082\u0001\u0000\u0000\u0000\u0085\u008a\u0005,\u0000\u0000"+
		"\u0086\u0087\u0005\u0002\u0000\u0000\u0087\u0088\u0003\u0012\t\u0000\u0088"+
		"\u0089\u0005\u0003\u0000\u0000\u0089\u008b\u0001\u0000\u0000\u0000\u008a"+
		"\u0086\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b"+
		"\u0011\u0001\u0000\u0000\u0000\u008c\u0091\u0003\u0014\n\u0000\u008d\u008e"+
		"\u0005\b\u0000\u0000\u008e\u0090\u0003\u0014\n\u0000\u008f\u008d\u0001"+
		"\u0000\u0000\u0000\u0090\u0093\u0001\u0000\u0000\u0000\u0091\u008f\u0001"+
		"\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000\u0092\u0013\u0001"+
		"\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000\u0094\u0095\u0005"+
		",\u0000\u0000\u0095\u0096\u0005\u0004\u0000\u0000\u0096\u0097\u0003\u001c"+
		"\u000e\u0000\u0097\u0015\u0001\u0000\u0000\u0000\u0098\u0099\u0005\f\u0000"+
		"\u0000\u0099\u009c\u0005,\u0000\u0000\u009a\u009b\u0005\u0004\u0000\u0000"+
		"\u009b\u009d\u0003\u001c\u000e\u0000\u009c\u009a\u0001\u0000\u0000\u0000"+
		"\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009e\u0001\u0000\u0000\u0000"+
		"\u009e\u009f\u0005\n\u0000\u0000\u009f\u00a0\u0003\"\u0011\u0000\u00a0"+
		"\u0017\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005\r\u0000\u0000\u00a2\u00a7"+
		"\u0003\u001a\r\u0000\u00a3\u00a4\u0005\b\u0000\u0000\u00a4\u00a6\u0003"+
		"\u001a\r\u0000\u00a5\u00a3\u0001\u0000\u0000\u0000\u00a6\u00a9\u0001\u0000"+
		"\u0000\u0000\u00a7\u00a5\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001\u0000"+
		"\u0000\u0000\u00a8\u00aa\u0001\u0000\u0000\u0000\u00a9\u00a7\u0001\u0000"+
		"\u0000\u0000\u00aa\u00ab\u0005\u000e\u0000\u0000\u00ab\u0019\u0001\u0000"+
		"\u0000\u0000\u00ac\u00ad\u0005,\u0000\u0000\u00ad\u001b\u0001\u0000\u0000"+
		"\u0000\u00ae\u00af\u0006\u000e\uffff\uffff\u0000\u00af\u00c2\u0003 \u0010"+
		"\u0000\u00b0\u00c2\u0005,\u0000\u0000\u00b1\u00b2\u0005,\u0000\u0000\u00b2"+
		"\u00b3\u0005\r\u0000\u0000\u00b3\u00b4\u0003\u001e\u000f\u0000\u00b4\u00b5"+
		"\u0005\u000e\u0000\u0000\u00b5\u00c2\u0001\u0000\u0000\u0000\u00b6\u00b7"+
		"\u0005\u0002\u0000\u0000\u00b7\u00bc\u0003\u001c\u000e\u0000\u00b8\u00b9"+
		"\u0005\b\u0000\u0000\u00b9\u00bb\u0003\u001c\u000e\u0000\u00ba\u00b8\u0001"+
		"\u0000\u0000\u0000\u00bb\u00be\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001"+
		"\u0000\u0000\u0000\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd\u00bf\u0001"+
		"\u0000\u0000\u0000\u00be\u00bc\u0001\u0000\u0000\u0000\u00bf\u00c0\u0005"+
		"\u0003\u0000\u0000\u00c0\u00c2\u0001\u0000\u0000\u0000\u00c1\u00ae\u0001"+
		"\u0000\u0000\u0000\u00c1\u00b0\u0001\u0000\u0000\u0000\u00c1\u00b1\u0001"+
		"\u0000\u0000\u0000\u00c1\u00b6\u0001\u0000\u0000\u0000\u00c2\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c3\u00c4\n\u0002\u0000\u0000\u00c4\u00c6\u0005\u000f"+
		"\u0000\u0000\u00c5\u00c3\u0001\u0000\u0000\u0000\u00c6\u00c9\u0001\u0000"+
		"\u0000\u0000\u00c7\u00c5\u0001\u0000\u0000\u0000\u00c7\u00c8\u0001\u0000"+
		"\u0000\u0000\u00c8\u001d\u0001\u0000\u0000\u0000\u00c9\u00c7\u0001\u0000"+
		"\u0000\u0000\u00ca\u00cf\u0003\u001c\u000e\u0000\u00cb\u00cc\u0005\b\u0000"+
		"\u0000\u00cc\u00ce\u0003\u001c\u000e\u0000\u00cd\u00cb\u0001\u0000\u0000"+
		"\u0000\u00ce\u00d1\u0001\u0000\u0000\u0000\u00cf\u00cd\u0001\u0000\u0000"+
		"\u0000\u00cf\u00d0\u0001\u0000\u0000\u0000\u00d0\u001f\u0001\u0000\u0000"+
		"\u0000\u00d1\u00cf\u0001\u0000\u0000\u0000\u00d2\u00d3\u0007\u0000\u0000"+
		"\u0000\u00d3!\u0001\u0000\u0000\u0000\u00d4\u00d5\u0006\u0011\uffff\uffff"+
		"\u0000\u00d5\u00dd\u0003$\u0012\u0000\u00d6\u00d7\u0005\u001a\u0000\u0000"+
		"\u00d7\u00dd\u0003\"\u0011\n\u00d8\u00d9\u0005\u001b\u0000\u0000\u00d9"+
		"\u00dd\u0003\"\u0011\t\u00da\u00dd\u00034\u001a\u0000\u00db\u00dd\u0003"+
		"<\u001e\u0000\u00dc\u00d4\u0001\u0000\u0000\u0000\u00dc\u00d6\u0001\u0000"+
		"\u0000\u0000\u00dc\u00d8\u0001\u0000\u0000\u0000\u00dc\u00da\u0001\u0000"+
		"\u0000\u0000\u00dc\u00db\u0001\u0000\u0000\u0000\u00dd\u0100\u0001\u0000"+
		"\u0000\u0000\u00de\u00df\n\b\u0000\u0000\u00df\u00e0\u0007\u0001\u0000"+
		"\u0000\u00e0\u00ff\u0003\"\u0011\t\u00e1\u00e2\n\u0007\u0000\u0000\u00e2"+
		"\u00e3\u0007\u0002\u0000\u0000\u00e3\u00ff\u0003\"\u0011\b\u00e4\u00e5"+
		"\n\u0006\u0000\u0000\u00e5\u00e6\u0007\u0003\u0000\u0000\u00e6\u00ff\u0003"+
		"\"\u0011\u0007\u00e7\u00e8\n\u0005\u0000\u0000\u00e8\u00e9\u0005$\u0000"+
		"\u0000\u00e9\u00ff\u0003\"\u0011\u0006\u00ea\u00eb\n\u0004\u0000\u0000"+
		"\u00eb\u00ec\u0005%\u0000\u0000\u00ec\u00ff\u0003\"\u0011\u0005\u00ed"+
		"\u00ee\n\u0003\u0000\u0000\u00ee\u00ef\u0005&\u0000\u0000\u00ef\u00ff"+
		"\u0003\"\u0011\u0004\u00f0\u00f1\n\r\u0000\u0000\u00f1\u00f2\u0005\u0017"+
		"\u0000\u0000\u00f2\u00ff\u0005,\u0000\u0000\u00f3\u00f4\n\f\u0000\u0000"+
		"\u00f4\u00f6\u0005\u0002\u0000\u0000\u00f5\u00f7\u00032\u0019\u0000\u00f6"+
		"\u00f5\u0001\u0000\u0000\u0000\u00f6\u00f7\u0001\u0000\u0000\u0000\u00f7"+
		"\u00f8\u0001\u0000\u0000\u0000\u00f8\u00ff\u0005\u0003\u0000\u0000\u00f9"+
		"\u00fa\n\u000b\u0000\u0000\u00fa\u00fb\u0005\u0018\u0000\u0000\u00fb\u00fc"+
		"\u0003\"\u0011\u0000\u00fc\u00fd\u0005\u0019\u0000\u0000\u00fd\u00ff\u0001"+
		"\u0000\u0000\u0000\u00fe\u00de\u0001\u0000\u0000\u0000\u00fe\u00e1\u0001"+
		"\u0000\u0000\u0000\u00fe\u00e4\u0001\u0000\u0000\u0000\u00fe\u00e7\u0001"+
		"\u0000\u0000\u0000\u00fe\u00ea\u0001\u0000\u0000\u0000\u00fe\u00ed\u0001"+
		"\u0000\u0000\u0000\u00fe\u00f0\u0001\u0000\u0000\u0000\u00fe\u00f3\u0001"+
		"\u0000\u0000\u0000\u00fe\u00f9\u0001\u0000\u0000\u0000\u00ff\u0102\u0001"+
		"\u0000\u0000\u0000\u0100\u00fe\u0001\u0000\u0000\u0000\u0100\u0101\u0001"+
		"\u0000\u0000\u0000\u0101#\u0001\u0000\u0000\u0000\u0102\u0100\u0001\u0000"+
		"\u0000\u0000\u0103\u010b\u0005,\u0000\u0000\u0104\u010b\u0003&\u0013\u0000"+
		"\u0105\u0106\u0005\u0002\u0000\u0000\u0106\u0107\u0003\"\u0011\u0000\u0107"+
		"\u0108\u0005\u0003\u0000\u0000\u0108\u010b\u0001\u0000\u0000\u0000\u0109"+
		"\u010b\u00030\u0018\u0000\u010a\u0103\u0001\u0000\u0000\u0000\u010a\u0104"+
		"\u0001\u0000\u0000\u0000\u010a\u0105\u0001\u0000\u0000\u0000\u010a\u0109"+
		"\u0001\u0000\u0000\u0000\u010b%\u0001\u0000\u0000\u0000\u010c\u0115\u0005"+
		"-\u0000\u0000\u010d\u0115\u0005.\u0000\u0000\u010e\u0115\u0005/\u0000"+
		"\u0000\u010f\u0115\u00050\u0000\u0000\u0110\u0115\u0003(\u0014\u0000\u0111"+
		"\u0115\u0003*\u0015\u0000\u0112\u0115\u0003.\u0017\u0000\u0113\u0115\u0005"+
		"\'\u0000\u0000\u0114\u010c\u0001\u0000\u0000\u0000\u0114\u010d\u0001\u0000"+
		"\u0000\u0000\u0114\u010e\u0001\u0000\u0000\u0000\u0114\u010f\u0001\u0000"+
		"\u0000\u0000\u0114\u0110\u0001\u0000\u0000\u0000\u0114\u0111\u0001\u0000"+
		"\u0000\u0000\u0114\u0112\u0001\u0000\u0000\u0000\u0114\u0113\u0001\u0000"+
		"\u0000\u0000\u0115\'\u0001\u0000\u0000\u0000\u0116\u011f\u0005\u0018\u0000"+
		"\u0000\u0117\u011c\u0003\"\u0011\u0000\u0118\u0119\u0005\b\u0000\u0000"+
		"\u0119\u011b\u0003\"\u0011\u0000\u011a\u0118\u0001\u0000\u0000\u0000\u011b"+
		"\u011e\u0001\u0000\u0000\u0000\u011c\u011a\u0001\u0000\u0000\u0000\u011c"+
		"\u011d\u0001\u0000\u0000\u0000\u011d\u0120\u0001\u0000\u0000\u0000\u011e"+
		"\u011c\u0001\u0000\u0000\u0000\u011f\u0117\u0001\u0000\u0000\u0000\u011f"+
		"\u0120\u0001\u0000\u0000\u0000\u0120\u0121\u0001\u0000\u0000\u0000\u0121"+
		"\u0122\u0005\u0019\u0000\u0000\u0122)\u0001\u0000\u0000\u0000\u0123\u012c"+
		"\u0005\u0006\u0000\u0000\u0124\u0129\u0003,\u0016\u0000\u0125\u0126\u0005"+
		"\b\u0000\u0000\u0126\u0128\u0003,\u0016\u0000\u0127\u0125\u0001\u0000"+
		"\u0000\u0000\u0128\u012b\u0001\u0000\u0000\u0000\u0129\u0127\u0001\u0000"+
		"\u0000\u0000\u0129\u012a\u0001\u0000\u0000\u0000\u012a\u012d\u0001\u0000"+
		"\u0000\u0000\u012b\u0129\u0001\u0000\u0000\u0000\u012c\u0124\u0001\u0000"+
		"\u0000\u0000\u012c\u012d\u0001\u0000\u0000\u0000\u012d\u012e\u0001\u0000"+
		"\u0000\u0000\u012e\u012f\u0005\u0007\u0000\u0000\u012f+\u0001\u0000\u0000"+
		"\u0000\u0130\u0131\u0003\"\u0011\u0000\u0131\u0132\u0005\u0004\u0000\u0000"+
		"\u0132\u0133\u0003\"\u0011\u0000\u0133-\u0001\u0000\u0000\u0000\u0134"+
		"\u0135\u0005\u0002\u0000\u0000\u0135\u0138\u0003\"\u0011\u0000\u0136\u0137"+
		"\u0005\b\u0000\u0000\u0137\u0139\u0003\"\u0011\u0000\u0138\u0136\u0001"+
		"\u0000\u0000\u0000\u0139\u013a\u0001\u0000\u0000\u0000\u013a\u0138\u0001"+
		"\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013b\u013c\u0001"+
		"\u0000\u0000\u0000\u013c\u013d\u0005\u0003\u0000\u0000\u013d/\u0001\u0000"+
		"\u0000\u0000\u013e\u013f\u0005,\u0000\u0000\u013f\u0141\u0005\u0002\u0000"+
		"\u0000\u0140\u0142\u00032\u0019\u0000\u0141\u0140\u0001\u0000\u0000\u0000"+
		"\u0141\u0142\u0001\u0000\u0000\u0000\u0142\u0143\u0001\u0000\u0000\u0000"+
		"\u0143\u0144\u0005\u0003\u0000\u0000\u01441\u0001\u0000\u0000\u0000\u0145"+
		"\u014a\u0003\"\u0011\u0000\u0146\u0147\u0005\b\u0000\u0000\u0147\u0149"+
		"\u0003\"\u0011\u0000\u0148\u0146\u0001\u0000\u0000\u0000\u0149\u014c\u0001"+
		"\u0000\u0000\u0000\u014a\u0148\u0001\u0000\u0000\u0000\u014a\u014b\u0001"+
		"\u0000\u0000\u0000\u014b3\u0001\u0000\u0000\u0000\u014c\u014a\u0001\u0000"+
		"\u0000\u0000\u014d\u014e\u0005(\u0000\u0000\u014e\u014f\u0003\"\u0011"+
		"\u0000\u014f\u0151\u0005\u0006\u0000\u0000\u0150\u0152\u00036\u001b\u0000"+
		"\u0151\u0150\u0001\u0000\u0000\u0000\u0152\u0153\u0001\u0000\u0000\u0000"+
		"\u0153\u0151\u0001\u0000\u0000\u0000\u0153\u0154\u0001\u0000\u0000\u0000"+
		"\u0154\u0155\u0001\u0000\u0000\u0000\u0155\u0156\u0005\u0007\u0000\u0000"+
		"\u01565\u0001\u0000\u0000\u0000\u0157\u0158\u0005)\u0000\u0000\u0158\u0159"+
		"\u00038\u001c\u0000\u0159\u015a\u0005\u0005\u0000\u0000\u015a\u015b\u0003"+
		"\"\u0011\u0000\u015b7\u0001\u0000\u0000\u0000\u015c\u015d\u0006\u001c"+
		"\uffff\uffff\u0000\u015d\u0162\u0005*\u0000\u0000\u015e\u0162\u0005,\u0000"+
		"\u0000\u015f\u0162\u0003&\u0013\u0000\u0160\u0162\u0003:\u001d\u0000\u0161"+
		"\u015c\u0001\u0000\u0000\u0000\u0161\u015e\u0001\u0000\u0000\u0000\u0161"+
		"\u015f\u0001\u0000\u0000\u0000\u0161\u0160\u0001\u0000\u0000\u0000\u0162"+
		"\u0168\u0001\u0000\u0000\u0000\u0163\u0164\n\u0001\u0000\u0000\u0164\u0165"+
		"\u0005+\u0000\u0000\u0165\u0167\u0003\"\u0011\u0000\u0166\u0163\u0001"+
		"\u0000\u0000\u0000\u0167\u016a\u0001\u0000\u0000\u0000\u0168\u0166\u0001"+
		"\u0000\u0000\u0000\u0168\u0169\u0001\u0000\u0000\u0000\u01699\u0001\u0000"+
		"\u0000\u0000\u016a\u0168\u0001\u0000\u0000\u0000\u016b\u016c\u0005,\u0000"+
		"\u0000\u016c\u0175\u0005\u0002\u0000\u0000\u016d\u0172\u00038\u001c\u0000"+
		"\u016e\u016f\u0005\b\u0000\u0000\u016f\u0171\u00038\u001c\u0000\u0170"+
		"\u016e\u0001\u0000\u0000\u0000\u0171\u0174\u0001\u0000\u0000\u0000\u0172"+
		"\u0170\u0001\u0000\u0000\u0000\u0172\u0173\u0001\u0000\u0000\u0000\u0173"+
		"\u0176\u0001\u0000\u0000\u0000\u0174\u0172\u0001\u0000\u0000\u0000\u0175"+
		"\u016d\u0001\u0000\u0000\u0000\u0175\u0176\u0001\u0000\u0000\u0000\u0176"+
		"\u0177\u0001\u0000\u0000\u0000\u0177\u0178\u0005\u0003\u0000\u0000\u0178"+
		";\u0001\u0000\u0000\u0000\u0179\u017a\u0005,\u0000\u0000\u017a\u017b\u0005"+
		"\u0005\u0000\u0000\u017b\u018b\u0003\"\u0011\u0000\u017c\u0185\u0005\u0002"+
		"\u0000\u0000\u017d\u0182\u0005,\u0000\u0000\u017e\u017f\u0005\b\u0000"+
		"\u0000\u017f\u0181\u0005,\u0000\u0000\u0180\u017e\u0001\u0000\u0000\u0000"+
		"\u0181\u0184\u0001\u0000\u0000\u0000\u0182\u0180\u0001\u0000\u0000\u0000"+
		"\u0182\u0183\u0001\u0000\u0000\u0000\u0183\u0186\u0001\u0000\u0000\u0000"+
		"\u0184\u0182\u0001\u0000\u0000\u0000\u0185\u017d\u0001\u0000\u0000\u0000"+
		"\u0185\u0186\u0001\u0000\u0000\u0000\u0186\u0187\u0001\u0000\u0000\u0000"+
		"\u0187\u0188\u0005\u0003\u0000\u0000\u0188\u0189\u0005\u0005\u0000\u0000"+
		"\u0189\u018b\u0003\"\u0011\u0000\u018a\u0179\u0001\u0000\u0000\u0000\u018a"+
		"\u017c\u0001\u0000\u0000\u0000\u018b=\u0001\u0000\u0000\u0000(AJOSXbf"+
		"msx\u0082\u008a\u0091\u009c\u00a7\u00bc\u00c1\u00c7\u00cf\u00dc\u00f6"+
		"\u00fe\u0100\u010a\u0114\u011c\u011f\u0129\u012c\u013a\u0141\u014a\u0153"+
		"\u0161\u0168\u0172\u0175\u0182\u0185\u018a";
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}