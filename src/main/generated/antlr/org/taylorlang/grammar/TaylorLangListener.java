// Generated from org/taylorlang/grammar/TaylorLang.g4 by ANTLR 4.13.1

package org.taylorlang.grammar;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TaylorLangParser}.
 */
public interface TaylorLangListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(TaylorLangParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(TaylorLangParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(TaylorLangParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(TaylorLangParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDecl(TaylorLangParser.FunctionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDecl(TaylorLangParser.FunctionDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void enterFunctionBody(TaylorLangParser.FunctionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void exitFunctionBody(TaylorLangParser.FunctionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#paramList}.
	 * @param ctx the parse tree
	 */
	void enterParamList(TaylorLangParser.ParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#paramList}.
	 * @param ctx the parse tree
	 */
	void exitParamList(TaylorLangParser.ParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(TaylorLangParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(TaylorLangParser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#typeDecl}.
	 * @param ctx the parse tree
	 */
	void enterTypeDecl(TaylorLangParser.TypeDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#typeDecl}.
	 * @param ctx the parse tree
	 */
	void exitTypeDecl(TaylorLangParser.TypeDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#unionType}.
	 * @param ctx the parse tree
	 */
	void enterUnionType(TaylorLangParser.UnionTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#unionType}.
	 * @param ctx the parse tree
	 */
	void exitUnionType(TaylorLangParser.UnionTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#productType}.
	 * @param ctx the parse tree
	 */
	void enterProductType(TaylorLangParser.ProductTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#productType}.
	 * @param ctx the parse tree
	 */
	void exitProductType(TaylorLangParser.ProductTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#positionedProductType}.
	 * @param ctx the parse tree
	 */
	void enterPositionedProductType(TaylorLangParser.PositionedProductTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#positionedProductType}.
	 * @param ctx the parse tree
	 */
	void exitPositionedProductType(TaylorLangParser.PositionedProductTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#namedProductType}.
	 * @param ctx the parse tree
	 */
	void enterNamedProductType(TaylorLangParser.NamedProductTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#namedProductType}.
	 * @param ctx the parse tree
	 */
	void exitNamedProductType(TaylorLangParser.NamedProductTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#namedFieldList}.
	 * @param ctx the parse tree
	 */
	void enterNamedFieldList(TaylorLangParser.NamedFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#namedFieldList}.
	 * @param ctx the parse tree
	 */
	void exitNamedFieldList(TaylorLangParser.NamedFieldListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#namedField}.
	 * @param ctx the parse tree
	 */
	void enterNamedField(TaylorLangParser.NamedFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#namedField}.
	 * @param ctx the parse tree
	 */
	void exitNamedField(TaylorLangParser.NamedFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#positionedFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPositionedFieldList(TaylorLangParser.PositionedFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#positionedFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPositionedFieldList(TaylorLangParser.PositionedFieldListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#valDecl}.
	 * @param ctx the parse tree
	 */
	void enterValDecl(TaylorLangParser.ValDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#valDecl}.
	 * @param ctx the parse tree
	 */
	void exitValDecl(TaylorLangParser.ValDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#typeParams}.
	 * @param ctx the parse tree
	 */
	void enterTypeParams(TaylorLangParser.TypeParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#typeParams}.
	 * @param ctx the parse tree
	 */
	void exitTypeParams(TaylorLangParser.TypeParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#typeParam}.
	 * @param ctx the parse tree
	 */
	void enterTypeParam(TaylorLangParser.TypeParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#typeParam}.
	 * @param ctx the parse tree
	 */
	void exitTypeParam(TaylorLangParser.TypeParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(TaylorLangParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(TaylorLangParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#typeArgs}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgs(TaylorLangParser.TypeArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#typeArgs}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgs(TaylorLangParser.TypeArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(TaylorLangParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(TaylorLangParser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TaylorLangParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TaylorLangParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(TaylorLangParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(TaylorLangParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#blockExpr}.
	 * @param ctx the parse tree
	 */
	void enterBlockExpr(TaylorLangParser.BlockExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#blockExpr}.
	 * @param ctx the parse tree
	 */
	void exitBlockExpr(TaylorLangParser.BlockExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#blockContent}.
	 * @param ctx the parse tree
	 */
	void enterBlockContent(TaylorLangParser.BlockContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#blockContent}.
	 * @param ctx the parse tree
	 */
	void exitBlockContent(TaylorLangParser.BlockContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(TaylorLangParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(TaylorLangParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#tupleLiteral}.
	 * @param ctx the parse tree
	 */
	void enterTupleLiteral(TaylorLangParser.TupleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#tupleLiteral}.
	 * @param ctx the parse tree
	 */
	void exitTupleLiteral(TaylorLangParser.TupleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#constructorCall}.
	 * @param ctx the parse tree
	 */
	void enterConstructorCall(TaylorLangParser.ConstructorCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#constructorCall}.
	 * @param ctx the parse tree
	 */
	void exitConstructorCall(TaylorLangParser.ConstructorCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#argList}.
	 * @param ctx the parse tree
	 */
	void enterArgList(TaylorLangParser.ArgListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#argList}.
	 * @param ctx the parse tree
	 */
	void exitArgList(TaylorLangParser.ArgListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void enterIfExpr(TaylorLangParser.IfExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#ifExpr}.
	 * @param ctx the parse tree
	 */
	void exitIfExpr(TaylorLangParser.IfExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void enterForExpr(TaylorLangParser.ForExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void exitForExpr(TaylorLangParser.ForExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#matchExpr}.
	 * @param ctx the parse tree
	 */
	void enterMatchExpr(TaylorLangParser.MatchExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#matchExpr}.
	 * @param ctx the parse tree
	 */
	void exitMatchExpr(TaylorLangParser.MatchExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void enterMatchCase(TaylorLangParser.MatchCaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void exitMatchCase(TaylorLangParser.MatchCaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(TaylorLangParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(TaylorLangParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#constructorPattern}.
	 * @param ctx the parse tree
	 */
	void enterConstructorPattern(TaylorLangParser.ConstructorPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#constructorPattern}.
	 * @param ctx the parse tree
	 */
	void exitConstructorPattern(TaylorLangParser.ConstructorPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link TaylorLangParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpr(TaylorLangParser.LambdaExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TaylorLangParser#lambdaExpr}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpr(TaylorLangParser.LambdaExprContext ctx);
}