package org.taylorlang.typechecker

import kotlinx.collections.immutable.toPersistentList
import org.taylorlang.ast.Type

/**
 * Core Robinson's unification algorithm implementation.
 * 
 * This class contains the pure unification algorithm logic, implementing the classic
 * most general unifier algorithm. It focuses solely on the algorithm mechanics
 * without concern for constraint management or substitution data structure operations.
 * 
 * Key responsibilities:
 * - Type unification using Robinson's algorithm rules
 * - Type-specific unification logic (generics, functions, tuples, etc.)
 * - Structural type equality checking
 * - Type compatibility analysis
 * 
 * The algorithm follows these standard unification rules:
 * 1. Identical types unify with empty substitution
 * 2. Type variable unification (with occurs check delegation)
 * 3. Constructor unification (same constructor, unify arguments)
 * 4. Failure for incompatible types
 */
internal class UnificationAlgorithm {
    
    /**
     * Unify two types under the given substitution context.
     * 
     * This is the core of Robinson's unification algorithm, implementing
     * the standard unification rules for different type combinations.
     * 
     * @param type1 The first type to unify
     * @param type2 The second type to unify
     * @param subst The current substitution context
     * @param occursChecker The occurs checker for infinite type detection
     * @return A substitution that unifies the two types
     * @throws UnificationError if the types cannot be unified
     */
    fun unifyTypes(
        type1: Type, 
        type2: Type, 
        subst: Substitution,
        occursChecker: OccursChecker
    ): Substitution {
        // Apply current substitution to both types first
        val t1 = subst.apply(type1)
        val t2 = subst.apply(type2)
        
        return when {
            // Rule 1: Identical types unify with empty substitution
            typesStructurallyEqual(t1, t2) -> Substitution.empty()
            
            // Rule 2: Type variable on the left
            t1 is Type.TypeVar -> unifyTypeVarWithType(TypeVar(t1.id), t2, occursChecker)
            t1 is Type.NamedType && isTypeVariable(t1) -> unifyTypeVarWithType(TypeVar(t1.name), t2, occursChecker)
            
            // Rule 3: Type variable on the right
            t2 is Type.TypeVar -> unifyTypeVarWithType(TypeVar(t2.id), t1, occursChecker)
            t2 is Type.NamedType && isTypeVariable(t2) -> unifyTypeVarWithType(TypeVar(t2.name), t1, occursChecker)
            
            // Rule 4: Generic types with same constructor
            t1 is Type.GenericType && t2 is Type.GenericType && t1.name == t2.name -> {
                unifyGenericTypes(t1, t2, subst, occursChecker)
            }
            
            // Rule 5: Union types with same name
            t1 is Type.UnionType && t2 is Type.UnionType && t1.name == t2.name -> {
                unifyUnionTypes(t1, t2, subst, occursChecker)
            }
            
            // Rule 6: Tuple types
            t1 is Type.TupleType && t2 is Type.TupleType -> {
                unifyTupleTypes(t1, t2, subst, occursChecker)
            }
            
            // Rule 7: Function types
            t1 is Type.FunctionType && t2 is Type.FunctionType -> {
                unifyFunctionTypes(t1, t2, subst, occursChecker)
            }
            
            // Rule 8: Nullable types
            t1 is Type.NullableType && t2 is Type.NullableType -> {
                unifyTypes(t1.baseType, t2.baseType, subst, occursChecker)
            }
            
            // Rule 9: Nullable type with non-nullable type (allow this unification)
            t1 is Type.NullableType && t2 !is Type.NullableType -> {
                unifyTypes(t1.baseType, t2, subst, occursChecker)
            }
            t1 !is Type.NullableType && t2 is Type.NullableType -> {
                unifyTypes(t1, t2.baseType, subst, occursChecker)
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
    private fun unifyTypeVarWithType(
        typeVar: TypeVar, 
        type: Type, 
        occursChecker: OccursChecker
    ): Substitution {
        // Occurs check: prevent infinite types
        if (occursChecker.occursCheck(typeVar, type)) {
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
        subst: Substitution,
        occursChecker: OccursChecker
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
        return unifyTypeList(type1.arguments.toList(), type2.arguments.toList(), subst, occursChecker)
    }
    
    /**
     * Unify two union types with the same name.
     */
    private fun unifyUnionTypes(
        type1: Type.UnionType,
        type2: Type.UnionType,
        subst: Substitution,
        occursChecker: OccursChecker
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
        return unifyTypeList(type1.typeArguments.toList(), type2.typeArguments.toList(), subst, occursChecker)
    }
    
    /**
     * Unify two tuple types.
     */
    private fun unifyTupleTypes(
        type1: Type.TupleType,
        type2: Type.TupleType,
        subst: Substitution,
        occursChecker: OccursChecker
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
        return unifyTypeList(type1.elementTypes.toList(), type2.elementTypes.toList(), subst, occursChecker)
    }
    
    /**
     * Unify two function types.
     */
    private fun unifyFunctionTypes(
        type1: Type.FunctionType,
        type2: Type.FunctionType,
        subst: Substitution,
        occursChecker: OccursChecker
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
            subst,
            occursChecker
        )
        
        // Unify return types
        currentSubst = unifyTypes(type1.returnType, type2.returnType, currentSubst, occursChecker)
            .compose(currentSubst)
        
        return currentSubst
    }
    
    /**
     * Unify corresponding elements in two type lists.
     */
    private fun unifyTypeList(
        types1: List<Type>,
        types2: List<Type>,
        subst: Substitution,
        occursChecker: OccursChecker
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
            val newSubst = unifyTypes(types1[i], types2[i], currentSubst, occursChecker)
            currentSubst = newSubst.compose(currentSubst)
        }
        
        return currentSubst
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
}