// Generated from org/taylorlang/grammar/TaylorLang.g4 by ANTLR 4.13.1

package org.taylorlang.grammar;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TaylorLangParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TaylorLangVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(TaylorLangParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(TaylorLangParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#functionDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDecl(TaylorLangParser.FunctionDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(TaylorLangParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#paramList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamList(TaylorLangParser.ParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(TaylorLangParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#typeDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDecl(TaylorLangParser.TypeDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#unionType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionType(TaylorLangParser.UnionTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#productType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProductType(TaylorLangParser.ProductTypeContext ctx);
	/**
<<<<<<< HEAD
	 * Visit a parse tree produced by {@link TaylorLangParser#positionedProductType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionedProductType(TaylorLangParser.PositionedProductTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#namedProductType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedProductType(TaylorLangParser.NamedProductTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#namedFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedFieldList(TaylorLangParser.NamedFieldListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#namedField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedField(TaylorLangParser.NamedFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#positionedFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionedFieldList(TaylorLangParser.PositionedFieldListContext ctx);
=======
	 * Visit a parse tree produced by {@link TaylorLangParser#fieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldList(TaylorLangParser.FieldListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(TaylorLangParser.FieldContext ctx);
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#valDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValDecl(TaylorLangParser.ValDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#typeParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParams(TaylorLangParser.TypeParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#typeParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParam(TaylorLangParser.TypeParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(TaylorLangParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#typeArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgs(TaylorLangParser.TypeArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(TaylorLangParser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(TaylorLangParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(TaylorLangParser.PrimaryContext ctx);
	/**
<<<<<<< HEAD
	 * Visit a parse tree produced by {@link TaylorLangParser#blockExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockExpr(TaylorLangParser.BlockExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#blockContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockContent(TaylorLangParser.BlockContentContext ctx);
	/**
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	 * Visit a parse tree produced by {@link TaylorLangParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(TaylorLangParser.LiteralContext ctx);
	/**
<<<<<<< HEAD
=======
	 * Visit a parse tree produced by {@link TaylorLangParser#listLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListLiteral(TaylorLangParser.ListLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#mapLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapLiteral(TaylorLangParser.MapLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#mapEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapEntry(TaylorLangParser.MapEntryContext ctx);
	/**
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	 * Visit a parse tree produced by {@link TaylorLangParser#tupleLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleLiteral(TaylorLangParser.TupleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#constructorCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorCall(TaylorLangParser.ConstructorCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#argList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgList(TaylorLangParser.ArgListContext ctx);
	/**
<<<<<<< HEAD
	 * Visit a parse tree produced by {@link TaylorLangParser#ifExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExpr(TaylorLangParser.IfExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#forExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForExpr(TaylorLangParser.ForExprContext ctx);
	/**
=======
>>>>>>> 703e8f0 (Implement TaylorLang compiler foundation with Kotlin)
	 * Visit a parse tree produced by {@link TaylorLangParser#matchExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchExpr(TaylorLangParser.MatchExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#matchCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchCase(TaylorLangParser.MatchCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(TaylorLangParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#constructorPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorPattern(TaylorLangParser.ConstructorPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link TaylorLangParser#lambdaExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpr(TaylorLangParser.LambdaExprContext ctx);
}