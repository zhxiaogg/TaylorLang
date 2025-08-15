# Git Commit Guidelines

## Mandatory Commit Protocol
ALL agents MUST commit their changes before exit, including docs, code, and any modifications.

## Commit Message Format
```
<type>: <description>
```

## Types
- `feat`: New feature implementation
- `fix`: Bug fixes  
- `docs`: Documentation updates
- `refactor`: Code restructuring
- `test`: Test additions/modifications
- `style`: Code formatting
- `perf`: Performance improvements

## Rules
1. **Use single quotes**: `git commit -m 'feat: implement pattern matching'`
2. **Keep under 50 characters** for the description
3. **Use imperative mood**: "add" not "added" or "adds"
4. **Be specific**: "fix VerifyError in Pair patterns" not "fix bug"
5. **Always push after commit**: `git push`

## Examples
```bash
git commit -m 'fix: resolve VerifyError in constructor patterns'
git commit -m 'docs: update JVM bytecode guidelines'
git commit -m 'feat: add wildcard pattern support'
git commit -m 'refactor: simplify pattern matching logic'
```

## Mandatory Before Exit
Every agent must:
1. Stage relevant changes: `git add .`
2. Commit with proper message
3. Push to remote: `git push`