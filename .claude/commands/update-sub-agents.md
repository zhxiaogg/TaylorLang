# Update Sub-Agents Command

## Overview
This command analyzes user intentions and updates sub-agent configurations to ensure they remain accurate, concise, and aligned with project needs.

## Process

### 1. Agent Discovery & Analysis
```
Claude:
- Read all agent files in .claude/agents/*.md
- Parse agent configurations (name, description, tools, model)
- Analyze current capabilities and responsibilities
- Identify potential gaps or redundancies
```

### 2. User Intent Analysis
```
Claude:
- Understand user's specific update requirements
- Determine which agents need modifications based on:
  - New project requirements
  - Changed toolsets or capabilities
  - Refined role definitions
  - Performance optimization needs
  - Clarity improvements
```

### 3. Agent Update Strategy
```
Claude determines update approach:
- Content refinement: Make descriptions more precise/concise
- Role clarification: Remove ambiguity in responsibilities
- Tool updates: Add/remove tools based on actual needs
- Model optimization: Adjust model selection for performance
- Example updates: Improve usage examples for clarity
```

### 4. Precision Updates
```
For each agent requiring updates:
- Maintain core agent identity and purpose
- Ensure descriptions are concise without losing essential information
- Remove redundant or unclear language
- Add specific, actionable examples
- Verify tool assignments match actual responsibilities
- Ensure no ambiguity in when to use each agent
```

### 5. Quality Assurance
```
Claude validates each updated agent:
- Description is clear and unambiguous
- Examples demonstrate proper usage scenarios
- Tool list matches agent capabilities
- No overlapping responsibilities with other agents
- Maintains consistency with project conventions
```

## Update Criteria

### Content Quality Standards
- **Conciseness**: Remove unnecessary words while preserving meaning
- **Precision**: Use exact, specific language over general terms
- **Clarity**: Eliminate ambiguous phrasing or instructions
- **Completeness**: Include all essential information for proper usage

### Role Definition Standards
- **Unique Purpose**: Each agent has distinct, non-overlapping responsibilities
- **Clear Boundaries**: Explicit when to use vs when not to use
- **Actionable Examples**: Realistic scenarios with clear decision rationale
- **Tool Alignment**: Tools match the agent's actual capabilities and needs

## Implementation Approach

### User Intent Recognition
Claude analyzes user requests for:
- Specific agent improvements requested
- Performance issues or confusion points
- New capabilities needed
- Role clarification needs
- General optimization requests

### Targeted Updates
Based on user intent, Claude:
1. Identifies specific agents requiring updates
2. Determines exact changes needed
3. Implements precise modifications
4. Validates changes maintain agent effectiveness
5. Ensures updates improve rather than complicate agent usage

## Output
- Updated agent configurations that are more concise and precise
- Maintained agent effectiveness and purpose
- Improved clarity for proper agent selection
- Enhanced examples and usage guidance
- Elimination of ambiguous or redundant content

## Usage
Run this command when sub-agents need refinement, clarification, or optimization based on project evolution or user feedback.