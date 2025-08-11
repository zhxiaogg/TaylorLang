package org.taylorlang.typechecker

import org.taylorlang.ast.*
import org.taylorlang.ast.visitor.BaseASTVisitor

/**
 * Information about a pattern match including variable bindings and covered variants.
 * 
 * This data class captures the result of pattern type checking, providing:
 * - Variable bindings introduced by the pattern
 * - Coverage information for exhaustiveness checking
 */
data class PatternInfo(
    val bindings: Map<String, Type>,
    val coveredVariants: Set<String>
)

/**
 * Visitor for type checking patterns using the visitor pattern.
 * 
 * This visitor handles pattern type checking for match expressions, including:
 * - Wildcard and identifier patterns
 * - Literal pattern matching
 * - Constructor pattern decomposition
 * - Guard pattern validation
 * - Variable binding extraction
 * - Exhaustiveness coverage analysis
 */
class PatternTypeChecker(
    private val context: TypeContext,
    private val expressionChecker: ExpressionTypeChecker
) : BaseASTVisitor<Result<PatternInfo>>() {
    
    override fun defaultResult(): Result<PatternInfo> {
        return Result.success(PatternInfo(
            bindings = emptyMap(),
            coveredVariants = emptySet()
        ))
    }
    
    override fun combine(first: Result<PatternInfo>, second: Result<PatternInfo>): Result<PatternInfo> {
        return first.mapCatching { firstInfo ->
            second.mapCatching { secondInfo ->
                PatternInfo(
                    bindings = firstInfo.bindings + secondInfo.bindings,
                    coveredVariants = firstInfo.coveredVariants + secondInfo.coveredVariants
                )
            }.getOrThrow()
        }
    }
    
    /**
     * Type check a pattern against a target type.
     * 
     * @param pattern The pattern to check
     * @param targetType The type that the pattern should match
     * @return Result containing pattern info or error
     */
    fun checkPattern(pattern: Pattern, targetType: Type): Result<PatternInfo> {
        return checkPatternInternal(pattern, targetType)
    }
    
    private fun checkPatternInternal(pattern: Pattern, targetType: Type): Result<PatternInfo> {
        return when (pattern) {
            is Pattern.WildcardPattern -> visitWildcardPatternInternal(pattern, targetType)
            is Pattern.IdentifierPattern -> visitIdentifierPatternInternal(pattern, targetType)
            is Pattern.LiteralPattern -> visitLiteralPatternInternal(pattern, targetType)
            is Pattern.ConstructorPattern -> visitConstructorPatternInternal(pattern, targetType)
            is Pattern.GuardPattern -> visitGuardPatternInternal(pattern, targetType)
        }
    }
    
    override fun visitPattern(node: Pattern): Result<PatternInfo> {
        return when (node) {
            is Pattern.WildcardPattern -> visitWildcardPattern(node)
            is Pattern.IdentifierPattern -> visitIdentifierPattern(node)
            is Pattern.LiteralPattern -> visitLiteralPattern(node)
            is Pattern.ConstructorPattern -> visitConstructorPattern(node)
            is Pattern.GuardPattern -> visitGuardPattern(node)
        }
    }
    
    override fun visitWildcardPattern(node: Pattern.WildcardPattern): Result<PatternInfo> {
        return defaultResult()
    }
    
    private fun visitWildcardPatternInternal(node: Pattern.WildcardPattern, targetType: Type): Result<PatternInfo> {
        // Wildcard matches anything and covers all variants for the target type
        val coveredVariants = when (targetType) {
            is Type.UnionType -> {
                val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                unionTypeDef?.getAllVariantNames() ?: emptySet()
            }
            else -> emptySet()
        }
        
        return Result.success(PatternInfo(
            bindings = emptyMap(),
            coveredVariants = coveredVariants
        ))
    }
    
    override fun visitIdentifierPattern(node: Pattern.IdentifierPattern): Result<PatternInfo> {
        return defaultResult()
    }
    
    private fun visitIdentifierPatternInternal(node: Pattern.IdentifierPattern, targetType: Type): Result<PatternInfo> {
        // Check if this identifier is actually a nullary constructor
        val typeName = when (targetType) {
            is Type.UnionType -> targetType.name
            is Type.GenericType -> targetType.name
            else -> null
        }
        
        if (typeName != null) {
            val unionTypeDef = context.lookupType(typeName) as? TypeDefinition.UnionTypeDef
            val nullaryVariant = unionTypeDef?.variants?.find { 
                it.name == node.name && it.isNullary() 
            }
            
            if (nullaryVariant != null) {
                // This identifier is actually a nullary constructor - treat as constructor pattern
                return Result.success(PatternInfo(
                    bindings = emptyMap(),
                    coveredVariants = setOf(node.name)
                ))
            }
        }
        
        // Regular identifier pattern - binds the entire value to a variable
        val coveredVariants = when (targetType) {
            is Type.UnionType -> {
                val unionTypeDef = context.lookupType(targetType.name) as? TypeDefinition.UnionTypeDef
                unionTypeDef?.getAllVariantNames() ?: emptySet()
            }
            else -> emptySet()
        }
        
        return Result.success(PatternInfo(
            bindings = mapOf(node.name to targetType),
            coveredVariants = coveredVariants
        ))
    }
    
    override fun visitLiteralPattern(node: Pattern.LiteralPattern): Result<PatternInfo> {
        return defaultResult()
    }
    
    private fun visitLiteralPatternInternal(node: Pattern.LiteralPattern, targetType: Type): Result<PatternInfo> {
        // Literal patterns must match the target type exactly
        val literalResult = node.literal.accept(expressionChecker)
        return literalResult.mapCatching { typedLiteral ->
            if (typesCompatible(typedLiteral.type, targetType)) {
                PatternInfo(
                    bindings = emptyMap(),
                    coveredVariants = emptySet() // Literals don't cover union variants
                )
            } else {
                throw TypeError.TypeMismatch(
                    expected = targetType,
                    actual = typedLiteral.type,
                    location = node.literal.sourceLocation
                )
            }
        }
    }
    
    override fun visitConstructorPattern(node: Pattern.ConstructorPattern): Result<PatternInfo> {
        return defaultResult()
    }
    
    private fun visitConstructorPatternInternal(node: Pattern.ConstructorPattern, targetType: Type): Result<PatternInfo> {
        // Constructor patterns must match a specific variant of a union type
        val typeName = when (targetType) {
            is Type.UnionType -> targetType.name
            is Type.GenericType -> targetType.name
            else -> {
                return Result.failure(TypeError.InvalidOperation(
                    "Constructor pattern can only be used with union types",
                    listOf(targetType),
                    node.sourceLocation
                ))
            }
        }
        
        val unionTypeDef = context.lookupType(typeName) as? TypeDefinition.UnionTypeDef
            ?: return Result.failure(TypeError.UndefinedType(
                typeName,
                node.sourceLocation
            ))
        
        val matchingVariant = unionTypeDef.findVariant(node.constructor)
            ?: return Result.failure(TypeError.UnresolvedSymbol(
                node.constructor,
                node.sourceLocation
            ))
        
        // Check that pattern arity matches variant arity
        if (node.patterns.size != matchingVariant.arity()) {
            return Result.failure(TypeError.ArityMismatch(
                expected = matchingVariant.arity(),
                actual = node.patterns.size,
                location = node.sourceLocation
            ))
        }
        
        // Type check nested patterns and collect bindings
        val allBindings = mutableMapOf<String, Type>()
        val errors = mutableListOf<TypeError>()
        
        for (i in node.patterns.indices) {
            val nestedPattern = node.patterns[i]
            val expectedFieldType = if (unionTypeDef.isGeneric()) {
                // Substitute type parameters with concrete types from the target
                val typeArguments = when (targetType) {
                    is Type.UnionType -> targetType.typeArguments.toList()
                    is Type.GenericType -> targetType.arguments.toList()
                    else -> emptyList()
                }
                substituteTypeParameters(
                    matchingVariant.fields[i],
                    unionTypeDef.typeParameters,
                    typeArguments
                )
            } else {
                matchingVariant.fields[i]
            }
            
            val nestedChecker = PatternTypeChecker(context, expressionChecker)
            val nestedResult = nestedChecker.checkPattern(nestedPattern, expectedFieldType)
            nestedResult.fold(
                onSuccess = { nestedInfo ->
                    allBindings.putAll(nestedInfo.bindings)
                },
                onFailure = { error ->
                    errors.add(when (error) {
                        is TypeError -> error
                        else -> TypeError.InvalidOperation(
                            error.message ?: "Unknown error",
                            emptyList(),
                            nestedPattern.sourceLocation
                        )
                    })
                }
            )
        }
        
        return if (errors.isNotEmpty()) {
            Result.failure(
                if (errors.size == 1) errors.first()
                else TypeError.MultipleErrors(errors)
            )
        } else {
            Result.success(PatternInfo(
                bindings = allBindings,
                coveredVariants = setOf(node.constructor)
            ))
        }
    }
    
    override fun visitGuardPattern(node: Pattern.GuardPattern): Result<PatternInfo> {
        return defaultResult()
    }
    
    private fun visitGuardPatternInternal(node: Pattern.GuardPattern, targetType: Type): Result<PatternInfo> {
        // Type check the inner pattern first
        val innerChecker = PatternTypeChecker(context, expressionChecker)
        val innerResult = innerChecker.checkPattern(node.pattern, targetType)
        
        return innerResult.mapCatching { innerInfo ->
            // Create context with pattern bindings for guard expression
            val guardContext = context.withVariables(innerInfo.bindings)
            val guardChecker = ExpressionTypeChecker(guardContext)
            
            // Type check the guard expression - must be Boolean
            val guardResult = node.guard.accept(guardChecker)
            guardResult.mapCatching { typedGuard ->
                if (typesCompatible(typedGuard.type, BuiltinTypes.BOOLEAN)) {
                    innerInfo // Return inner pattern info
                } else {
                    throw TypeError.TypeMismatch(
                        expected = BuiltinTypes.BOOLEAN,
                        actual = typedGuard.type,
                        location = node.guard.sourceLocation
                    )
                }
            }.getOrThrow()
        }
    }
    
    // =============================================================================
    // Helper Methods
    // =============================================================================
    
    private fun typesCompatible(type1: Type, type2: Type): Boolean {
        // Migrated to use centralized TypeOperations for consistent type comparison
        return TypeOperations.areEqual(type1, type2)
    }
    
    private fun substituteTypeParameters(
        type: Type,
        typeParameters: List<String>,
        typeArguments: List<Type>
    ): Type {
        val substitutionMap = typeParameters.zip(typeArguments).toMap()
        return substituteType(type, substitutionMap)
    }
    
    private fun substituteType(type: Type, substitutionMap: Map<String, Type>): Type {
        return when (type) {
            is Type.NamedType -> {
                // Replace type parameter with concrete type if found in substitution map
                substitutionMap[type.name] ?: type
            }
            is Type.GenericType -> {
                // Recursively substitute type arguments
                Type.GenericType(
                    name = type.name,
                    arguments = type.arguments.map { substituteType(it, substitutionMap) }
                        .let { kotlinx.collections.immutable.persistentListOf(*it.toTypedArray()) },
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.UnionType -> {
                // Recursively substitute type arguments
                Type.UnionType(
                    name = type.name,
                    typeArguments = type.typeArguments.map { substituteType(it, substitutionMap) }
                        .let { kotlinx.collections.immutable.persistentListOf(*it.toTypedArray()) },
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.NullableType -> {
                Type.NullableType(
                    baseType = substituteType(type.baseType, substitutionMap),
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.TupleType -> {
                Type.TupleType(
                    elementTypes = type.elementTypes.map { substituteType(it, substitutionMap) }
                        .let { kotlinx.collections.immutable.persistentListOf(*it.toTypedArray()) },
                    sourceLocation = type.sourceLocation
                )
            }
            is Type.FunctionType -> {
                Type.FunctionType(
                    parameterTypes = type.parameterTypes.map { substituteType(it, substitutionMap) }
                        .let { kotlinx.collections.immutable.persistentListOf(*it.toTypedArray()) },
                    returnType = substituteType(type.returnType, substitutionMap),
                    sourceLocation = type.sourceLocation
                )
            }
            // For type variables, return as-is (should not be substituted here)
            is Type.TypeVar -> type
            // For primitive types, return as-is
            is Type.PrimitiveType -> type
        }
    }
}