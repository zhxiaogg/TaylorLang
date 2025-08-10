package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

/**
 * Unification errors that can occur during constraint solving.
 */
sealed class UnificationError : Exception() {
    /**
     * Error when two types cannot be unified.
     */
    data class TypeMismatch(
        val type1: Type,
        val type2: Type,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Cannot unify types: $type1 and $type2${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when occurs check fails (infinite type detected).
     */
    data class InfiniteType(
        val typeVar: TypeVar,
        val type: Type,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Infinite type: ${typeVar.id} occurs in $type${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when type constructor arity doesn't match.
     */
    data class ArityMismatch(
        val constructor: String,
        val expected: Int,
        val actual: Int,
        val location: SourceLocation? = null
    ) : UnificationError() {
        override val message: String
            get() = "Arity mismatch for $constructor: expected $expected arguments, got $actual${location?.let { " at $it" } ?: ""}"
    }
    
    /**
     * Error when constraint solving fails.
     */
    data class ConstraintSolvingFailed(
        val constraint: Constraint,
        override val cause: UnificationError
    ) : UnificationError() {
        override val message: String
            get() = "Failed to solve constraint $constraint: ${cause.message}"
    }
}

/**
 * Unification algorithm implementation based on Robinson's algorithm.
 * 
 * This class implements the core unification algorithm for type inference,
 * solving type constraints to produce type substitutions. The algorithm
 * follows these key principles:
 * 
 * 1. **Robinson's Unification**: Uses the classic most general unifier algorithm
 * 2. **Occurs Check**: Prevents infinite types like T = List[T]
 * 3. **Compositional**: Builds substitutions incrementally through constraint solving
 * 4. **Error Handling**: Provides detailed error messages for debugging
 * 
 * The unification process works by:
 * 1. Taking a set of type constraints
 * 2. Iteratively solving each constraint to build up a substitution
 * 3. Applying substitutions to remaining constraints
 * 4. Returning the final substitution or an error
 * 
 * Example usage:
 * ```kotlin
 * val constraints = ConstraintSet.of(
 *     Constraint.Equality(Type.TypeVar("T"), BuiltinTypes.INT),
 *     Constraint.Equality(Type.GenericType("List", persistentListOf(Type.TypeVar("T"))), someListType)
 * )
 * val result = Unifier.solve(constraints)
 * ```
 */
class Unifier {
    
    companion object {
        /**
         * Solve a set of type constraints to produce a type substitution.
         * 
         * This is the main entry point for constraint-based type inference.
         * It processes all constraints in the set and returns either a
         * successful substitution or an error.
         * 
         * @param constraints The set of constraints to solve
         * @return Result containing the substitution or error
         */
        fun solve(constraints: ConstraintSet): Result<Substitution> {
            return try {
                val unifier = Unifier()
                val substitution = unifier.solveConstraints(constraints)
                Result.success(substitution)
            } catch (e: UnificationError) {
                Result.failure(e)
            }
        }
        
        /**
         * Unify two types directly without constraint solving.
         * 
         * This is useful for type checking scenarios where you need
         * to check if two specific types can be unified.
         * 
         * @param type1 The first type to unify
         * @param type2 The second type to unify
         * @return Result containing the unifying substitution or error
         */
        fun unify(type1: Type, type2: Type): Result<Substitution> {
            return try {
                val unifier = Unifier()
                val substitution = unifier.unifyTypes(type1, type2, Substitution.empty())
                Result.success(substitution)
            } catch (e: UnificationError) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Solve all constraints in the constraint set.
     * 
     * This method processes constraints iteratively, building up a substitution
     * and applying it to remaining constraints until all are solved.
     * 
     * @param constraints The constraints to solve
     * @return The resulting substitution
     * @throws UnificationError if any constraint cannot be solved
     */
    private fun solveConstraints(constraints: ConstraintSet): Substitution {
        var currentSubst = Substitution.empty()
        var remainingConstraints = constraints.toList()
        
        // Process constraints until none remain
        while (remainingConstraints.isNotEmpty()) {
            val constraint = remainingConstraints.first()
            remainingConstraints = remainingConstraints.drop(1)
            
            try {
                // Solve the current constraint
                val newSubst = solveConstraint(constraint, currentSubst)
                
                // Compose with existing substitution
                currentSubst = newSubst.compose(currentSubst)
                
                // Apply the new substitution to remaining constraints
                remainingConstraints = remainingConstraints.map { c ->
                    applySubstitutionToConstraint(c, newSubst)
                }
                
            } catch (e: UnificationError) {
                throw UnificationError.ConstraintSolvingFailed(constraint, e)
            }
        }
        
        return currentSubst
    }
    
    /**
     * Solve a single constraint.
     * 
     * @param constraint The constraint to solve
     * @param currentSubst The current substitution context
     * @return A substitution that satisfies this constraint
     * @throws UnificationError if the constraint cannot be solved
     */
    private fun solveConstraint(constraint: Constraint, currentSubst: Substitution): Substitution {
        return when (constraint) {
            is Constraint.Equality -> {
                // For equality constraints, unify the two types
                unifyTypes(constraint.left, constraint.right, currentSubst)
            }
            
            is Constraint.Subtype -> {
                // For subtype constraints, handle numeric promotion and other subtyping relations
                handleSubtypeConstraint(constraint.subtype, constraint.supertype, currentSubst)
            }
            
            is Constraint.Instance -> {
                // For instance constraints, instantiate the type scheme
                // and unify with the type variable
                val instantiatedType = instantiateTypeScheme(constraint.scheme)
                unifyTypes(
                    Type.NamedType(constraint.typeVar.id), // Convert TypeVar to Type
                    instantiatedType,
                    currentSubst
                )
            }
        }
    }
    
    /**
     * Unify two types under the given substitution context.
     * 
     * This is the core of Robinson's unification algorithm, implementing
     * the standard unification rules for different type combinations.
     * 
     * @param type1 The first type to unify
     * @param type2 The second type to unify
     * @param subst The current substitution context
     * @return A substitution that unifies the two types
     * @throws UnificationError if the types cannot be unified
     */
    private fun unifyTypes(type1: Type, type2: Type, subst: Substitution): Substitution {
        // Apply current substitution to both types first
        val t1 = subst.apply(type1)
        val t2 = subst.apply(type2)
        
        return when {
            // Rule 1: Identical types unify with empty substitution
            typesStructurallyEqual(t1, t2) -> Substitution.empty()
            
            // Rule 2: Type variable on the left
            t1 is Type.TypeVar -> unifyTypeVarWithType(TypeVar(t1.id), t2)
            t1 is Type.NamedType && isTypeVariable(t1) -> unifyTypeVarWithType(TypeVar(t1.name), t2)
            
            // Rule 3: Type variable on the right
            t2 is Type.TypeVar -> unifyTypeVarWithType(TypeVar(t2.id), t1)
            t2 is Type.NamedType && isTypeVariable(t2) -> unifyTypeVarWithType(TypeVar(t2.name), t1)
            
            // Rule 4: Generic types with same constructor
            t1 is Type.GenericType && t2 is Type.GenericType && t1.name == t2.name -> {
                unifyGenericTypes(t1, t2, subst)
            }
            
            // Rule 5: Union types with same name
            t1 is Type.UnionType && t2 is Type.UnionType && t1.name == t2.name -> {
                unifyUnionTypes(t1, t2, subst)
            }
            
            // Rule 6: Tuple types
            t1 is Type.TupleType && t2 is Type.TupleType -> {
                unifyTupleTypes(t1, t2, subst)
            }
            
            // Rule 7: Function types
            t1 is Type.FunctionType && t2 is Type.FunctionType -> {
                unifyFunctionTypes(t1, t2, subst)
            }
            
            // Rule 8: Nullable types
            t1 is Type.NullableType && t2 is Type.NullableType -> {
                unifyTypes(t1.baseType, t2.baseType, subst)
            }
            
            // Rule 9: Nullable type with non-nullable type (allow this unification)
            t1 is Type.NullableType && t2 !is Type.NullableType -> {
                unifyTypes(t1.baseType, t2, subst)
            }
            t1 !is Type.NullableType && t2 is Type.NullableType -> {
                unifyTypes(t1, t2.baseType, subst)
            }
            
            // Rule 10: Cannot unify
            else -> throw UnificationError.TypeMismatch(t1, t2, t1.sourceLocation ?: t2.sourceLocation)
        }
    }
    
    /**
     * Unify a type variable with a type.
     * 
     * This implements the core type variable unification rule with occurs checking.
     */
    private fun unifyTypeVarWithType(typeVar: TypeVar, type: Type): Substitution {
        // Occurs check: prevent infinite types
        if (occursCheck(typeVar, type)) {
            throw UnificationError.InfiniteType(typeVar, type)
        }
        
        // Create substitution mapping the type variable to the type
        return Substitution.single(typeVar, type)
    }
    
    /**
     * Unify two generic types with the same type constructor.
     */
    private fun unifyGenericTypes(
        type1: Type.GenericType,
        type2: Type.GenericType,
        subst: Substitution
    ): Substitution {
        // Check arity
        if (type1.arguments.size != type2.arguments.size) {
            throw UnificationError.ArityMismatch(
                type1.name,
                type1.arguments.size,
                type2.arguments.size
            )
        }
        
        // Unify corresponding type arguments
        return unifyTypeList(type1.arguments.toList(), type2.arguments.toList(), subst)
    }
    
    /**
     * Unify two union types with the same name.
     */
    private fun unifyUnionTypes(
        type1: Type.UnionType,
        type2: Type.UnionType,
        subst: Substitution
    ): Substitution {
        // Check arity
        if (type1.typeArguments.size != type2.typeArguments.size) {
            throw UnificationError.ArityMismatch(
                type1.name,
                type1.typeArguments.size,
                type2.typeArguments.size
            )
        }
        
        // Unify corresponding type arguments
        return unifyTypeList(type1.typeArguments.toList(), type2.typeArguments.toList(), subst)
    }
    
    /**
     * Unify two tuple types.
     */
    private fun unifyTupleTypes(
        type1: Type.TupleType,
        type2: Type.TupleType,
        subst: Substitution
    ): Substitution {
        // Check arity
        if (type1.elementTypes.size != type2.elementTypes.size) {
            throw UnificationError.ArityMismatch(
                "Tuple",
                type1.elementTypes.size,
                type2.elementTypes.size
            )
        }
        
        // Unify corresponding element types
        return unifyTypeList(type1.elementTypes.toList(), type2.elementTypes.toList(), subst)
    }
    
    /**
     * Unify two function types.
     */
    private fun unifyFunctionTypes(
        type1: Type.FunctionType,
        type2: Type.FunctionType,
        subst: Substitution
    ): Substitution {
        // Check parameter count
        if (type1.parameterTypes.size != type2.parameterTypes.size) {
            throw UnificationError.ArityMismatch(
                "Function",
                type1.parameterTypes.size,
                type2.parameterTypes.size
            )
        }
        
        // Unify parameter types
        var currentSubst = unifyTypeList(
            type1.parameterTypes.toList(),
            type2.parameterTypes.toList(),
            subst
        )
        
        // Unify return types
        currentSubst = unifyTypes(type1.returnType, type2.returnType, currentSubst)
            .compose(currentSubst)
        
        return currentSubst
    }
    
    /**
     * Unify corresponding elements in two type lists.
     */
    private fun unifyTypeList(
        types1: List<Type>,
        types2: List<Type>,
        subst: Substitution
    ): Substitution {
        if (types1.size != types2.size) {
            throw UnificationError.ArityMismatch(
                "TypeList",
                types1.size,
                types2.size
            )
        }
        
        var currentSubst = subst
        for (i in types1.indices) {
            val newSubst = unifyTypes(types1[i], types2[i], currentSubst)
            currentSubst = newSubst.compose(currentSubst)
        }
        
        return currentSubst
    }
    
    /**
     * Occurs check: determine if a type variable occurs in a type.
     * 
     * This prevents infinite types like T = List[T] by checking if
     * the type variable being unified appears in the type it's being
     * unified with.
     * 
     * @param typeVar The type variable to check for
     * @param type The type to check in
     * @return true if the type variable occurs in the type
     */
    private fun occursCheck(typeVar: TypeVar, type: Type): Boolean {
        return when (type) {
            is Type.TypeVar -> typeVar.id == type.id
            
            is Type.NamedType -> {
                // Check if this NamedType represents the same type variable
                isTypeVariable(type) && typeVar.id == type.name
            }
            
            is Type.GenericType -> {
                type.arguments.any { occursCheck(typeVar, it) }
            }
            
            is Type.UnionType -> {
                type.typeArguments.any { occursCheck(typeVar, it) }
            }
            
            is Type.NullableType -> {
                occursCheck(typeVar, type.baseType)
            }
            
            is Type.TupleType -> {
                type.elementTypes.any { occursCheck(typeVar, it) }
            }
            
            is Type.FunctionType -> {
                type.parameterTypes.any { occursCheck(typeVar, it) } ||
                occursCheck(typeVar, type.returnType)
            }
            
            is Type.PrimitiveType -> false
        }
    }
    
    /**
     * Check if two types are structurally equal (ignoring source locations).
     */
    private fun typesStructurallyEqual(type1: Type, type2: Type): Boolean {
        return when {
            type1 is Type.PrimitiveType && type2 is Type.PrimitiveType ->
                type1.name == type2.name
                
            type1 is Type.NamedType && type2 is Type.NamedType ->
                type1.name == type2.name
                
            type1 is Type.TypeVar && type2 is Type.TypeVar ->
                type1.id == type2.id
                
            type1 is Type.GenericType && type2 is Type.GenericType ->
                type1.name == type2.name &&
                type1.arguments.size == type2.arguments.size &&
                type1.arguments.zip(type2.arguments).all { (t1, t2) ->
                    typesStructurallyEqual(t1, t2)
                }
                
            type1 is Type.UnionType && type2 is Type.UnionType ->
                type1.name == type2.name &&
                type1.typeArguments.size == type2.typeArguments.size &&
                type1.typeArguments.zip(type2.typeArguments).all { (t1, t2) ->
                    typesStructurallyEqual(t1, t2)
                }
                
            type1 is Type.NullableType && type2 is Type.NullableType ->
                typesStructurallyEqual(type1.baseType, type2.baseType)
                
            type1 is Type.TupleType && type2 is Type.TupleType ->
                type1.elementTypes.size == type2.elementTypes.size &&
                type1.elementTypes.zip(type2.elementTypes).all { (t1, t2) ->
                    typesStructurallyEqual(t1, t2)
                }
                
            type1 is Type.FunctionType && type2 is Type.FunctionType ->
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (t1, t2) ->
                    typesStructurallyEqual(t1, t2)
                } && typesStructurallyEqual(type1.returnType, type2.returnType)
                
            else -> false
        }
    }
    
    /**
     * Check if a NamedType represents a type variable.
     * 
     * This is a heuristic based on naming conventions.
     */
    private fun isTypeVariable(type: Type.NamedType): Boolean {
        // Type variables are typically single uppercase letters or start with T
        // Also consider common built-in type names as NOT type variables
        val builtinTypeNames = setOf("Int", "String", "Boolean", "Double", "Float", "Long", "Unit")
        
        return !builtinTypeNames.contains(type.name) && (
               type.name.length == 1 && type.name[0].isUpperCase() ||
               type.name.startsWith("T") && type.name.drop(1).all { it.isDigit() }
        )
    }
    
    /**
     * Apply a substitution to a constraint.
     */
    private fun applySubstitutionToConstraint(
        constraint: Constraint,
        subst: Substitution
    ): Constraint {
        return when (constraint) {
            is Constraint.Equality -> {
                Constraint.Equality(
                    subst.apply(constraint.left),
                    subst.apply(constraint.right),
                    constraint.location
                )
            }
            
            is Constraint.Subtype -> {
                Constraint.Subtype(
                    subst.apply(constraint.subtype),
                    subst.apply(constraint.supertype),
                    constraint.location
                )
            }
            
            is Constraint.Instance -> {
                // For instance constraints, we apply substitution to the type scheme
                val substitutedScheme = TypeScheme(
                    constraint.scheme.quantifiedVars, // Don't substitute bound variables
                    subst.apply(constraint.scheme.type)
                )
                Constraint.Instance(
                    constraint.typeVar,
                    substitutedScheme,
                    constraint.location
                )
            }
        }
    }
    
    /**
     * Instantiate a type scheme by replacing quantified variables with fresh type variables.
     */
    private fun instantiateTypeScheme(scheme: TypeScheme): Type {
        if (scheme.quantifiedVars.isEmpty()) {
            return scheme.type
        }
        
        // Create fresh type variables for each quantified variable
        val instantiationSubst = scheme.quantifiedVars.associate { quantifiedVar ->
            quantifiedVar to Type.NamedType(TypeVar.fresh().id)
        }
        
        // Apply instantiation substitution to the scheme type
        return instantiationSubst.entries.fold(scheme.type) { type, (quantifiedVar, freshType) ->
            Substitution.single(quantifiedVar, freshType).apply(type)
        }
    }
    
    /**
     * Handle subtype constraints, including numeric promotion.
     * 
     * @param subtype The subtype (left side of <:)
     * @param supertype The supertype (right side of <:)  
     * @param currentSubst The current substitution context
     * @return A substitution that satisfies the subtype constraint
     * @throws UnificationError if the subtype relation cannot be established
     */
    private fun handleSubtypeConstraint(subtype: Type, supertype: Type, currentSubst: Substitution): Substitution {
        // Apply current substitution to both types
        val substSubtype = currentSubst.apply(subtype)
        val substSupertype = currentSubst.apply(supertype)
        
        return when {
            // Reflexivity: T <: T
            typesEqual(substSubtype, substSupertype) -> Substitution.empty()
            
            // Numeric promotion: INT <: DOUBLE
            substSubtype == BuiltinTypes.INT && substSupertype == BuiltinTypes.DOUBLE -> {
                Substitution.empty() // This is always valid
            }
            
            // Type variables can be made subtypes through substitution
            substSubtype is Type.NamedType && substSupertype != substSubtype -> {
                // If subtype is a type variable, substitute it with supertype
                if (isTypeVariable(substSubtype)) {
                    Substitution.single(TypeVar.named(substSubtype.name), substSupertype)
                } else {
                    // Non-variable types must have their subtype relation checked
                    // For now, fall back to equality for other cases
                    unifyTypes(substSubtype, substSupertype, currentSubst)
                }
            }
            
            substSupertype is Type.NamedType && isTypeVariable(substSupertype) -> {
                // If supertype is a type variable, this is more complex
                // For numeric types, we can constrain the supertype variable
                if (substSubtype == BuiltinTypes.INT) {
                    // T1 where INT <: T1, so T1 should be at least DOUBLE
                    Substitution.single(TypeVar.named(substSupertype.name), BuiltinTypes.DOUBLE)
                } else {
                    // General case: make supertype equal to subtype (conservative)
                    Substitution.single(TypeVar.named(substSupertype.name), substSubtype)
                }
            }
            
            else -> {
                // No subtype relation can be established - this should fail
                throw UnificationError.TypeMismatch(
                    substSubtype, 
                    substSupertype
                )
            }
        }
    }
    
    /**
     * Check if a type represents a type variable (used for substitution).
     */
    private fun isTypeVariable(type: Type): Boolean {
        return type is Type.NamedType && type.name.matches(Regex("T\\d+"))
    }
    
    /**
     * Check if two types are structurally equal.
     */
    private fun typesEqual(type1: Type, type2: Type): Boolean {
        return when {
            type1 is Type.PrimitiveType && type2 is Type.PrimitiveType -> 
                type1.name == type2.name
            type1 is Type.NamedType && type2 is Type.NamedType -> 
                type1.name == type2.name
            type1 is Type.GenericType && type2 is Type.GenericType -> 
                type1.name == type2.name && type1.arguments.size == type2.arguments.size &&
                type1.arguments.zip(type2.arguments).all { (a1, a2) -> typesEqual(a1, a2) }
            type1 is Type.TupleType && type2 is Type.TupleType -> 
                type1.elementTypes.size == type2.elementTypes.size &&
                type1.elementTypes.zip(type2.elementTypes).all { (t1, t2) -> typesEqual(t1, t2) }
            type1 is Type.NullableType && type2 is Type.NullableType ->
                typesEqual(type1.baseType, type2.baseType)
            type1 is Type.UnionType && type2 is Type.UnionType ->
                type1.name == type2.name && type1.typeArguments.size == type2.typeArguments.size &&
                type1.typeArguments.zip(type2.typeArguments).all { (a1, a2) -> typesEqual(a1, a2) }
            type1 is Type.FunctionType && type2 is Type.FunctionType ->
                typesEqual(type1.returnType, type2.returnType) &&
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (p1, p2) -> typesEqual(p1, p2) }
            else -> type1 == type2
        }
    }
}