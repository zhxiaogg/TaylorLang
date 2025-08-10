package org.taylorlang.ast.visitor

import org.taylorlang.ast.*

/**
 * Collection of utility visitors that demonstrate the value of the visitor pattern.
 * These visitors provide reusable functionality that would otherwise require duplicated
 * traversal code across different components like TypeChecker, ConstraintCollector, etc.
 */

/**
 * Collects all identifiers referenced in an AST subtree.
 * Useful for variable usage analysis, dependency tracking, and scope analysis.
 */
class IdentifierCollector : BaseASTVisitor<Set<String>>() {
    override fun defaultResult(): Set<String> = emptySet()
    
    override fun combine(first: Set<String>, second: Set<String>): Set<String> = first + second
    
    override fun visitIdentifier(node: Identifier): Set<String> = setOf(node.name)
    
    override fun visitPropertyAccess(node: PropertyAccess): Set<String> {
        val targetIdentifiers = node.target.accept(this)
        // Property name itself is not an identifier reference in this context
        return targetIdentifiers
    }
    
    // Don't collect identifiers from patterns - they are bindings, not references
    override fun visitIdentifierPattern(node: Pattern.IdentifierPattern): Set<String> = emptySet()
}

/**
 * Collects all type references in an AST subtree.
 * Useful for dependency analysis, import resolution, and type checking.
 */
class TypeReferenceCollector : BaseASTVisitor<Set<String>>() {
    override fun defaultResult(): Set<String> = emptySet()
    
    override fun combine(first: Set<String>, second: Set<String>): Set<String> = first + second
    
    override fun visitPrimitiveType(node: Type.PrimitiveType): Set<String> = setOf(node.name)
    override fun visitNamedType(node: Type.NamedType): Set<String> = setOf(node.name)
    override fun visitGenericType(node: Type.GenericType): Set<String> {
        val typeArgs = node.arguments.map { it.accept(this) }.fold(emptySet<String>()) { acc, set -> acc + set }
        return setOf(node.name) + typeArgs
    }
    override fun visitUnionTypeRef(node: Type.UnionType): Set<String> {
        val typeArgs = node.typeArguments.map { it.accept(this) }.fold(emptySet<String>()) { acc, set -> acc + set }
        return setOf(node.name) + typeArgs
    }
}

/**
 * Counts nodes of specific types for complexity analysis.
 * Useful for code metrics, complexity analysis, and performance estimation.
 */
class ComplexityAnalyzer : BaseASTVisitor<ComplexityMetrics>() {
    override fun defaultResult(): ComplexityMetrics = ComplexityMetrics()
    
    override fun combine(first: ComplexityMetrics, second: ComplexityMetrics): ComplexityMetrics {
        return ComplexityMetrics(
            expressions = first.expressions + second.expressions,
            binaryOps = first.binaryOps + second.binaryOps,
            functionCalls = first.functionCalls + second.functionCalls,
            matchExpressions = first.matchExpressions + second.matchExpressions,
            ifExpressions = first.ifExpressions + second.ifExpressions,
            lambdas = first.lambdas + second.lambdas,
            maxDepth = maxOf(first.maxDepth, second.maxDepth)
        )
    }
    
    override fun visitIdentifier(node: Identifier): ComplexityMetrics {
        val base = super.visitIdentifier(node)
        return base.copy(expressions = base.expressions + 1)
    }
    
    override fun visitLiteral(node: Literal): ComplexityMetrics {
        val base = super.visitLiteral(node)
        return base.copy(expressions = base.expressions + 1)
    }
    
    override fun visitBinaryOp(node: BinaryOp): ComplexityMetrics {
        val base = super.visitBinaryOp(node)
        return base.copy(binaryOps = base.binaryOps + 1)
    }
    
    override fun visitFunctionCall(node: FunctionCall): ComplexityMetrics {
        val base = super.visitFunctionCall(node)
        return base.copy(functionCalls = base.functionCalls + 1)
    }
    
    override fun visitMatchExpression(node: MatchExpression): ComplexityMetrics {
        val base = super.visitMatchExpression(node)
        return base.copy(matchExpressions = base.matchExpressions + 1)
    }
    
    override fun visitIfExpression(node: IfExpression): ComplexityMetrics {
        val base = super.visitIfExpression(node)
        return base.copy(ifExpressions = base.ifExpressions + 1)
    }
    
    override fun visitLambdaExpression(node: LambdaExpression): ComplexityMetrics {
        val base = super.visitLambdaExpression(node)
        return base.copy(lambdas = base.lambdas + 1)
    }
}

/**
 * Metrics collected by ComplexityAnalyzer
 */
data class ComplexityMetrics(
    val expressions: Int = 0,
    val binaryOps: Int = 0,
    val functionCalls: Int = 0,
    val matchExpressions: Int = 0,
    val ifExpressions: Int = 0,
    val lambdas: Int = 0,
    val maxDepth: Int = 0
) {
    /**
     * Calculate a simple complexity score based on the metrics
     */
    val complexityScore: Int
        get() = expressions + (binaryOps * 2) + (functionCalls * 3) + 
                (matchExpressions * 4) + (ifExpressions * 2) + (lambdas * 3)
}

/**
 * Validates that all type references are properly defined.
 * Useful for type checking and early error detection.
 */
class TypeValidator(private val definedTypes: Set<String>) : BaseASTVisitor<List<String>>() {
    override fun defaultResult(): List<String> = emptyList()
    
    override fun combine(first: List<String>, second: List<String>): List<String> = first + second
    
    override fun visitNamedType(node: Type.NamedType): List<String> {
        return if (node.name !in definedTypes && !isPrimitiveType(node.name)) {
            listOf("Undefined type: ${node.name} at ${node.sourceLocation}")
        } else {
            emptyList()
        }
    }
    
    override fun visitGenericType(node: Type.GenericType): List<String> {
        val baseError = if (node.name !in definedTypes && !isPrimitiveType(node.name)) {
            listOf("Undefined generic type: ${node.name} at ${node.sourceLocation}")
        } else {
            emptyList()
        }
        
        val argErrors = node.arguments.flatMap { it.accept(this) }
        return baseError + argErrors
    }
    
    private fun isPrimitiveType(name: String): Boolean {
        return name in setOf("Int", "Float", "String", "Bool", "Unit", "List", "Option", "Array", "Set", "Map")
    }
}

/**
 * Extracts all function signatures from a program.
 * Useful for building symbol tables and generating documentation.
 */
class FunctionSignatureExtractor : BaseASTVisitor<List<FunctionSignature>>() {
    override fun defaultResult(): List<FunctionSignature> = emptyList()
    
    override fun combine(first: List<FunctionSignature>, second: List<FunctionSignature>): List<FunctionSignature> {
        return first + second
    }
    
    override fun visitFunctionDecl(node: FunctionDecl): List<FunctionSignature> {
        val signature = FunctionSignature(
            name = node.name,
            typeParameters = node.typeParams.toList(),
            parameters = node.parameters.map { param ->
                ParameterInfo(param.name, param.type)
            },
            returnType = node.returnType,
            sourceLocation = node.sourceLocation
        )
        return listOf(signature)
    }
}

/**
 * Information about a function signature extracted from the AST
 */
data class FunctionSignature(
    val name: String,
    val typeParameters: List<String>,
    val parameters: List<ParameterInfo>,
    val returnType: Type?,
    val sourceLocation: SourceLocation?
)

/**
 * Information about a function parameter
 */
data class ParameterInfo(
    val name: String,
    val type: Type?
)

/**
 * Detects unused variables in function bodies.
 * Useful for code quality analysis and optimization.
 */
class UnusedVariableDetector : BaseASTVisitor<UnusedVariableReport>() {
    private val declaredVariables = mutableSetOf<String>()
    private val usedVariables = mutableSetOf<String>()
    
    override fun defaultResult(): UnusedVariableReport = UnusedVariableReport()
    
    override fun combine(first: UnusedVariableReport, second: UnusedVariableReport): UnusedVariableReport {
        return UnusedVariableReport(
            declared = first.declared + second.declared,
            used = first.used + second.used
        )
    }
    
    override fun visitParameter(node: Parameter): UnusedVariableReport {
        declaredVariables.add(node.name)
        val base = super.visitParameter(node)
        return base.copy(declared = base.declared + node.name)
    }
    
    override fun visitValDecl(node: ValDecl): UnusedVariableReport {
        declaredVariables.add(node.name)
        val base = super.visitValDecl(node)
        return base.copy(declared = base.declared + node.name)
    }
    
    override fun visitIdentifierPattern(node: Pattern.IdentifierPattern): UnusedVariableReport {
        declaredVariables.add(node.name)
        val base = super.visitIdentifierPattern(node)
        return base.copy(declared = base.declared + node.name)
    }
    
    override fun visitIdentifier(node: Identifier): UnusedVariableReport {
        usedVariables.add(node.name)
        val base = super.visitIdentifier(node)
        return base.copy(used = base.used + node.name)
    }
}

/**
 * Report of declared and used variables
 */
data class UnusedVariableReport(
    val declared: Set<String> = emptySet(),
    val used: Set<String> = emptySet()
) {
    val unused: Set<String> get() = declared - used
}