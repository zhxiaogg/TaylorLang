# Code Review Guidelines

## MANDATORY ENFORCEMENT POLICY

**ZERO TOLERANCE**: All rules in this document are mandatory. Any violation MUST result in immediate rejection of the work. No exceptions.

**BUILD/TEST FAILURES**: Any code that causes build failures or test failures MUST be rejected immediately, regardless of whether the issue was previously introduced or not. The codebase must remain in a working state at all times.

**PROJECT BLOCKING**: Code reviewers MUST block all project progress until violations are fixed. No code with violations may be merged or accepted.

## Quality Standards

### File Structure
- **500-line limit**: No file exceeds 500 lines - break into focused modules
- **Single responsibility**: Each file serves one clear purpose
- **Logical organization**: Related functions grouped together
- **Clear naming**: File names reflect their primary responsibility

### Function Design
- **Single purpose**: Functions do one thing well
- **Clear naming**: Function names describe behavior, not implementation
- **Parameter limit**: Maximum 5 parameters per function
- **Nesting limit**: Maximum 3-4 levels of nesting
- **Length limit**: Functions under 50 lines preferred

### Pattern Matching Requirements
- **Exhaustive patterns**: All pattern matching MUST be exhaustive - cover every possible case
- **No else branches**: NEVER use `else` branches when exhaustive pattern matching is possible
- **Explicit unimplemented cases**: For unsupported branches, throw explicit errors like `TODO()` or `NotImplementedError()`
- **Prevent silent failures**: Ensure no unexpected behavior when running tests due to missing cases

### Variable and Naming
- **Self-documenting**: Names explain intent without comments
- **Consistent conventions**: Follow established patterns in codebase
- **Meaningful distinctions**: Avoid similar names for different purposes
- **Boolean clarity**: Use positive, clear boolean names (`isValid` not `notInvalid`)

## Code Quality Checks

### Critical Issues (Must Fix)
- **Security vulnerabilities**: Input validation, SQL injection, XSS prevention
- **Memory leaks**: Resource cleanup, proper disposal patterns
- **Race conditions**: Thread safety, synchronization issues
- **Data integrity**: Null checks, boundary validation
- **Error propagation**: Unhandled exceptions, silent failures

### Important Issues (Should Fix)
- **Performance bottlenecks**: O(n²) algorithms, redundant operations
- **Code duplication**: Repeated logic without abstraction
- **Coupling issues**: Tight dependencies between modules
- **Missing error handling**: Inadequate exception management
- **Inconsistent patterns**: Deviations from established conventions

### Suggestions (Nice to Have)
- **Readability improvements**: Better variable names, clearer structure
- **Optimization opportunities**: Minor performance enhancements
- **Documentation additions**: Helpful comments for complex logic
- **Pattern applications**: Better use of design patterns
- **Code simplification**: Reducing complexity where possible

## Review Process

### Initial Assessment
1. **Repository cleanliness**: Verify no temporary files, debug code, or IDE artifacts
2. **File size check**: Verify 500-line limit compliance
3. **Structure overview**: Assess organization and modularity
4. **Dependency analysis**: Check import statements and coupling
5. **Naming scan**: Verify self-documenting code principles

### Detailed Analysis
1. **Function complexity**: Evaluate each function's cognitive load
2. **Error handling**: Check exception management and edge cases
3. **Security review**: Identify potential vulnerabilities
4. **Performance review**: Spot inefficient algorithms or operations
5. **Test coverage**: Assess testability and existing test quality

### Documentation Review
1. **Comment quality**: Comments explain 'why', not 'what'
2. **API documentation**: Public interfaces are well-documented
3. **README accuracy**: Project documentation reflects current state
4. **Code examples**: Documentation includes working examples

## Feedback Standards

### Issue Categorization
- **Critical**: Security, correctness, major performance issues
- **Important**: Maintainability, moderate performance, best practices
- **Suggestion**: Style, minor optimizations, alternative approaches

### Feedback Format
```
[CRITICAL/IMPORTANT/SUGGESTION]: Brief description
Location: file_path:line_number
Issue: Specific problem description
Recommendation: Concrete improvement suggestion
Rationale: Why this change matters
```

### Positive Reinforcement
- Highlight well-designed code patterns
- Acknowledge good naming and structure
- Recognize proper error handling
- Praise clear documentation

## Acceptance Criteria

### MANDATORY REQUIREMENTS (Must Pass - REJECTION IF ANY FAIL)
- [ ] **BUILDS SUCCESSFULLY**: Project builds without any errors
- [ ] **ALL TESTS PASS**: No test failures of any kind
- [ ] **No files exceed 500 lines**: Immediate rejection if violated
- [ ] **All functions have single, clear responsibilities**: No exceptions
- [ ] **No critical security or correctness issues**: Zero tolerance
- [ ] **Proper error handling for all edge cases**: Must be comprehensive
- [ ] **Consistent with project conventions**: No deviations allowed
- [ ] **Exhaustive pattern matching**: All pattern matches must be exhaustive with no `else` branches
- [ ] **Explicit unimplemented cases**: Unsupported branches must throw explicit errors, not fail silently

### Code Should Have
- [ ] Self-documenting variable and function names
- [ ] Comments explaining complex business logic
- [ ] Reasonable performance characteristics
- [ ] Minimal code duplication
- [ ] Clear separation of concerns

### Repository Cleanliness (MANDATORY)
- [ ] **No temporary files**: .tmp, .temp, .bak, .swp, .DS_Store files excluded
- [ ] **No IDE files**: .idea/, .vscode/, *.iml files excluded  
- [ ] **No build artifacts**: target/, build/, dist/, out/ directories excluded
- [ ] **No debug code**: console.log, print statements, debug imports removed
- [ ] **No commented code**: Dead code completely removed, not commented out
- [ ] **Clean git status**: Working directory clean before commits
- [ ] **Proper .gitignore**: All temporary and generated files properly ignored

### Commit Standards
- [ ] Commit messages use single quotes: `git commit -m 'commit message'`
- [ ] Messages explain 'why' not just 'what'
- [ ] Atomic commits with focused changes
- [ ] Repository clean before every commit
- [ ] All temporary files excluded from commits

## Common Patterns to Flag

### Anti-Patterns
- **God objects**: Classes with too many responsibilities
- **Long parameter lists**: Functions with more than 5 parameters
- **Deep nesting**: More than 4 levels of indentation
- **Magic numbers**: Unexplained constants throughout code
- **Shotgun surgery**: Changes requiring edits across many files
- **Non-exhaustive patterns**: Using `else` branches instead of explicit pattern matching
- **Silent pattern failures**: Missing cases that cause unexpected behavior during testing
- **Monolithic bytecode generators**: Single files handling multiple bytecode generation concerns
- **Debug file contamination**: Temporary .taylor files, debug artifacts committed to repository
- **Type inference inconsistency**: Identifier expressions not checking variable slot types
- **Mega files**: Files exceeding 500 lines - ExpressionBytecodeGenerator (1106 lines), BytecodeGenerator (851 lines) require immediate refactoring
- **Test failure tolerance**: Accepting partial test success rates - zero tolerance policy means 100% pass rate required
- **Arithmetic type inference defects**: Int+Int operations returning Double instead of Int indicates type promotion bugs
- **Refactoring regression risks**: Large architectural changes must preserve exact functional behavior - type inference logic extraction particularly prone to behavioral changes
- **Pattern matching coordinator violations**: When extracting specialized generators, ensure all Literal subtypes are handled exhaustively (TupleLiteral, NullLiteral often missed)
- **Component integration failures**: Refactored coordinator patterns must maintain all original integration points (TryExpression pattern compiler dependencies)

### Code Smells
- **Feature envy**: Classes accessing other classes' data excessively
- **Data clumps**: Same parameters passed together repeatedly
- **Primitive obsession**: Using primitives instead of small objects
- **Refused bequest**: Subclasses not using inherited functionality
- **Comments smell**: Comments explaining what code does instead of why

## Project-Specific Standards

### Git Workflow
- Single quotes in all commit messages
- Descriptive commit messages explaining business value
- No direct commits to main branch
- Pull requests require code review approval

### Architecture Compliance
- Follow patterns defined in `docs/architect-guidelines.md`
- Respect module boundaries and dependencies
- Use established error handling patterns
- Maintain consistency with existing code style

### Testing Requirements (MANDATORY)
- **ALL TESTS MUST PASS**: Zero tolerance for any test failures
- **BUILD MUST SUCCEED**: Zero tolerance for any build failures  
- Unit tests for all public functions
- Integration tests for critical workflows
- Test names describe behavior, not implementation
- Adequate coverage of edge cases and error conditions

## REJECTION CRITERIA

**IMMEDIATE REJECTION REQUIRED FOR:**
- Any build failure (compilation errors, missing dependencies, etc.)
- Any test failure (unit tests, integration tests, any automated tests)
- Files exceeding 500 lines
- Functions exceeding 50 lines
- Security vulnerabilities of any severity
- Missing error handling for edge cases
- Violations of established coding conventions
- Code that introduces technical debt
- **Repository contamination**: Temporary files, debug code, commented code, IDE files
- **Dirty working directory**: Uncommitted changes, untracked temporary files
- **Missing .gitignore entries**: Generated files not properly excluded
- **Non-exhaustive pattern matching**: Using `else` branches when exhaustive patterns are possible
- **Silent pattern failures**: Missing cases that could cause unexpected test behavior
- **Mega file violations**: Files > 1000 lines require immediate breaking into modules
- **Type system regressions**: Arithmetic operations returning incorrect types (Int+Int≠Double)
- **Test suite degradation**: Any reduction from 100% pass rate is blocking
- **Refactoring functional regression**: Type inference behavior changes during architectural extraction
- **Incomplete pattern matching extraction**: Missing cases in refactored generators (TupleLiteral, NullLiteral)
- **Integration dependency breaks**: Coordinator patterns losing original component connections

**NO EXCEPTIONS POLICY**: Previous issues do not excuse new violations. All code must meet standards regardless of existing codebase state.