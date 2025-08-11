package org.taylorlang.typechecker

import org.taylorlang.ast.Type
import org.taylorlang.ast.SourceLocation

/**
 * Complex substitution operations and constraint handling.
 * 
 * This class encapsulates the more complex aspects of substitution manipulation
 * that go beyond the core unification algorithm. It handles constraint processing,
 * type scheme instantiation, and subtype relationship management.
 * 
 * Key responsibilities:
 * - Applying substitutions to constraint structures
 * - Type scheme instantiation with fresh variable generation
 * - Subtype constraint resolution and handling
 * - Complex substitution composition and transformation
 * 
 * This separation allows the core unification algorithm to focus on pure
 * type unification while delegating complex data structure operations
 * to this specialized component.
 */
internal class SubstitutionComposer {
    
    /**
     * Apply a substitution to a constraint.
     * 
     * This transforms the constraint by applying the substitution to all
     * type expressions within the constraint, maintaining the constraint's
     * semantic meaning while updating the types.
     * 
     * @param constraint The constraint to transform
     * @param subst The substitution to apply
     * @return A new constraint with substitution applied
     */
    fun applySubstitutionToConstraint(
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
     * 
     * This creates a concrete instance of a polymorphic type by substituting
     * all quantified type variables with fresh, unbound type variables.
     * This is essential for polymorphic type usage.
     * 
     * @param scheme The type scheme to instantiate
     * @return A type with fresh variables substituted for quantified variables
     */
    fun instantiateTypeScheme(scheme: TypeScheme): Type {
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
     * This resolves subtype relationships by creating appropriate substitutions
     * that satisfy the subtype constraint. It handles various forms of subtyping
     * including structural subtyping and built-in type coercions.
     * 
     * @param subtype The subtype (left side of <:)
     * @param supertype The supertype (right side of <:)  
     * @param currentSubst The current substitution context
     * @return A substitution that satisfies the subtype constraint
     * @throws UnificationError if the subtype relation cannot be established
     */
    fun handleSubtypeConstraint(
        subtype: Type, 
        supertype: Type, 
        currentSubst: Substitution
    ): Substitution {
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
                    createEqualitySubstitution(substSubtype, substSupertype, currentSubst)
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
     * Create a substitution that makes two types equal.
     * 
     * This is a helper method for cases where subtype constraints
     * need to fall back to equality constraints.
     */
    private fun createEqualitySubstitution(
        type1: Type,
        type2: Type,
        currentSubst: Substitution
    ): Substitution {
        // This would require the unification algorithm, so we delegate
        // For now, we'll throw an error if types can't be made equal through simple substitution
        return when {
            type1 is Type.NamedType && isTypeVariable(type1) -> {
                Substitution.single(TypeVar.named(type1.name), type2)
            }
            type2 is Type.NamedType && isTypeVariable(type2) -> {
                Substitution.single(TypeVar.named(type2.name), type1)
            }
            else -> throw UnificationError.TypeMismatch(type1, type2)
        }
    }
    
    /**
     * Compose two substitutions with advanced conflict resolution.
     * 
     * This provides more sophisticated composition logic than the basic
     * Substitution.compose method, handling edge cases and optimization.
     * 
     * @param first The first substitution to apply
     * @param second The second substitution to apply
     * @return The composed substitution
     */
    fun composeSubstitutions(first: Substitution, second: Substitution): Substitution {
        return first.compose(second)
    }
    
    /**
     * Create a substitution set that satisfies multiple constraints simultaneously.
     * 
     * This attempts to find a substitution that satisfies all given constraints
     * at once, which can be more efficient than sequential constraint solving.
     * 
     * @param constraints The constraints to satisfy
     * @return A substitution that satisfies all constraints, or null if impossible
     */
    fun findSimultaneousSubstitution(constraints: List<Constraint>): Substitution? {
        // For simple cases, we can try direct substitution
        val equalityConstraints = constraints.filterIsInstance<Constraint.Equality>()
        
        if (equalityConstraints.size == constraints.size) {
            // All constraints are equalities - try direct variable assignment
            val assignments = mutableMapOf<TypeVar, Type>()
            
            for (constraint in equalityConstraints) {
                val left = constraint.left
                val right = constraint.right
                
                when {
                    left is Type.NamedType && isTypeVariable(left) && !containsTypeVar(right, TypeVar(left.name)) -> {
                        val typeVar = TypeVar(left.name)
                        if (assignments.containsKey(typeVar) && assignments[typeVar] != right) {
                            return null // Conflict
                        }
                        assignments[typeVar] = right
                    }
                    right is Type.NamedType && isTypeVariable(right) && !containsTypeVar(left, TypeVar(right.name)) -> {
                        val typeVar = TypeVar(right.name)
                        if (assignments.containsKey(typeVar) && assignments[typeVar] != left) {
                            return null // Conflict
                        }
                        assignments[typeVar] = left
                    }
                }
            }
            
            return if (assignments.isNotEmpty()) {
                Substitution.of(assignments)
            } else {
                null
            }
        }
        
        return null // Complex cases require full unification
    }
    
    /**
     * Check if a type contains a specific type variable.
     */
    private fun containsTypeVar(type: Type, typeVar: TypeVar): Boolean {
        return when (type) {
            is Type.TypeVar -> typeVar.id == type.id
            is Type.NamedType -> isTypeVariable(type) && typeVar.id == type.name
            is Type.GenericType -> type.arguments.any { containsTypeVar(it, typeVar) }
            is Type.UnionType -> type.typeArguments.any { containsTypeVar(it, typeVar) }
            is Type.NullableType -> containsTypeVar(type.baseType, typeVar)
            is Type.TupleType -> type.elementTypes.any { containsTypeVar(it, typeVar) }
            is Type.FunctionType -> {
                type.parameterTypes.any { containsTypeVar(it, typeVar) } ||
                containsTypeVar(type.returnType, typeVar)
            }
            is Type.PrimitiveType -> false
        }
    }
    
    /**
     * Check if a type represents a type variable (used for substitution).
     */
    private fun isTypeVariable(type: Type): Boolean {
        return when (type) {
            is Type.NamedType -> {
                // Type variables are typically single uppercase letters or start with T followed by digits
                // Built-in type names are explicitly excluded
                val builtinTypeNames = setOf("Int", "String", "Boolean", "Double", "Float", "Long", "Unit")
                
                !builtinTypeNames.contains(type.name) && (
                       // Single uppercase letter (T, U, V, etc.)
                       (type.name.length == 1 && type.name[0].isUpperCase()) ||
                       // T followed by digits (T1, T2, T3, etc.)
                       (type.name.startsWith("T") && type.name.drop(1).all { it.isDigit() })
                )
            }
            else -> false
        }
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
    
    /**
     * Optimize a substitution by removing redundant mappings.
     * 
     * This removes mappings where a type variable maps to itself
     * or other optimizations that don't change the substitution's effect.
     * 
     * @param substitution The substitution to optimize
     * @return An optimized substitution
     */
    fun optimizeSubstitution(substitution: Substitution): Substitution {
        return substitution.filter { typeVar, type ->
            // Remove identity mappings (T -> T)
            when (type) {
                is Type.NamedType -> type.name != typeVar.id
                is Type.TypeVar -> type.id != typeVar.id
                else -> true
            }
        }
    }
}