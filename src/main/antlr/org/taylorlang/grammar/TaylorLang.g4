grammar TaylorLang;

@header {
package org.taylorlang.grammar;
}

// Entry point
program: statement* EOF;

// Statements
statement
    : functionDecl
    | typeDecl
    | valDecl
    | varDecl
    | assignment
    | returnStatement
    | expression
    ;

// Function declarations: fun add(x: Int, y: Int): Int { return x + y }
functionDecl
    : 'fun' IDENTIFIER typeParams? '(' paramList? ')' (':' type)? functionBody
    ;

functionBody
    : '{' statement* '}'
    | '=>' expression  // Still support single expression syntax
    ;

// Return statement
returnStatement
    : 'return' expression?
    ;

paramList: param (',' param)*;
param: IDENTIFIER (':' type)?;

// Type declarations: type Result<T,E> = Ok(T) | Error(E)
typeDecl
    : 'type' IDENTIFIER typeParams? '=' unionType
    ;

unionType: productType ('|' productType)*;
productType: positionedProductType | namedProductType;
positionedProductType: IDENTIFIER ('(' positionedFieldList ')')?;
namedProductType: IDENTIFIER '(' namedFieldList ')';

namedFieldList: namedField (',' namedField)*;
namedField: IDENTIFIER ':' type;
positionedFieldList: type (',' type)*;

// Variable declarations: val x = 42, var y: Int = 10
valDecl: 'val' IDENTIFIER (':' type)? '=' expression;
varDecl: 'var' IDENTIFIER (':' type)? '=' expression;

// Assignment statements: x = newValue
assignment: IDENTIFIER '=' expression;

// Type parameters: <T, E>
typeParams: '<' typeParam (',' typeParam)* '>';
typeParam: IDENTIFIER;

// Types
type
    : primitiveType
    | IDENTIFIER                           // Named type
    | IDENTIFIER '<' typeArgs '>'          // Generic type
    | type '?'                            // Nullable type
    | '(' type (',' type)* ')'            // Tuple type
    ;

typeArgs: type (',' type)*;

primitiveType
    : 'Int'
    | 'Long' 
    | 'Float'
    | 'Double'
    | 'Boolean'
    | 'String'
    | 'Unit'
    ;

// Expressions
expression
    : primary
    | expression '.' IDENTIFIER           // Property access
    | expression '(' argList? ')'         // Function call
    | expression '[' expression ']'       // Index access
    | '-' expression                      // Unary minus
    | '!' expression                      // Logical not
    | expression ('*' | '/' | '%') expression  // Multiplicative
    | expression ('+' | '-') expression   // Additive
    | expression ('<' | '<=' | '>' | '>=' | '==' | '!=') expression  // Relational
    | expression '&&' expression          // Logical and
    | expression '||' expression          // Logical or
    | expression '?:' expression          // Null coalescing
    | ifExpr                             // If expression
    | whileExpr                          // While loop
    | forExpr                            // For loop
    | matchExpr                           // Pattern matching
    | lambdaExpr                          // Lambda expression
    ;

// Primary expressions
primary
    : IDENTIFIER                          // Variable reference
    | literal                             // Literals
    | '(' expression ')'                  // Parentheses
    | constructorCall                     // Type constructor
    | blockExpr                           // Block expression
    ;

// Block expressions: { expr1; expr2; ... }  
blockExpr: '{' blockContent '}';

// Block content - statements with semicolons or final expression
blockContent: (statement ';')* expression?;

// Literals
literal
    : IntLiteral
    | FloatLiteral
    | StringLiteral
    | BooleanLiteral
    | tupleLiteral
    | 'null'
    ;

tupleLiteral: '(' expression (',' expression)+ ')';

// Constructor calls: Ok(value)
constructorCall: IDENTIFIER '(' argList? ')';

// Function arguments
argList: expression (',' expression)*;

// If expressions: if (condition) expr else expr
// Supports both single expressions and block expressions
ifExpr: 'if' '(' expression ')' expression ('else' expression)?;

// While loops: while (condition) { body }
whileExpr: 'while' '(' expression ')' expression;

// For loops: for (item in collection) { body }
forExpr: 'for' '(' IDENTIFIER 'in' expression ')' expression;

// Pattern matching: match expr { case pattern => expr }
matchExpr: 'match' expression '{' matchCase+ '}';
matchCase: 'case' pattern '=>' expression;

pattern
    : '_'                                 // Wildcard
    | IDENTIFIER                          // Variable binding
    | literal                            // Literal pattern
    | constructorPattern                 // Constructor pattern
    | listPattern                        // List pattern
    | pattern 'if' expression           // Guard pattern
    ;

constructorPattern: IDENTIFIER '(' (pattern (',' pattern)*)? ')';

// List patterns: [], [x], [x, y], [first, ...rest]
listPattern
    : '[' ']'                                           // Empty list
    | '[' pattern (',' pattern)* ']'                   // Fixed-length list
    | '[' pattern (',' pattern)* ',' '...' IDENTIFIER ']'  // Head/tail pattern
    ;

// Lambda expressions: x => x * 2, (x, y) => x + y
lambdaExpr
    : IDENTIFIER '=>' expression
    | '(' (IDENTIFIER (',' IDENTIFIER)*)? ')' '=>' expression
    ;

// Lexer rules (keywords first, then identifiers)
BooleanLiteral: 'true' | 'false';
IntLiteral: [0-9]+;
FloatLiteral: [0-9]+ '.' [0-9]+;
StringLiteral: '"' (~["\r\n] | '\\' .)* '"';
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;

// Comments
LineComment: '//' ~[\r\n]* -> skip;
BlockComment: '/*' .*? '*/' -> skip;

// Whitespace
WS: [ \t\r\n]+ -> skip;