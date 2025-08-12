# Two-Agent Development Workflow

## Overview
This command creates a continuous development workflow with Claude orchestrating between two agents:
- **@agent-tech-lead-architect**: Project owner and task manager  
- **@agent-kotlin-java-engineer**: Implementation engineer

## Claude's Orchestration Process

Claude drives the entire workflow by coordinating between the agents:

### 1. Get Task from Tech Lead
```
Claude → Launch @agent-tech-lead-architect:
- Read README.md and docs/project/ for current status
- Identify next priority task from available work
- Create specific task assignment with clear requirements
- Return structured task specification
```

### 2. Assign Task to Engineer
```
Claude → Launch @agent-kotlin-java-engineer with task from step 1:
- Pass complete task specification from tech lead
- Engineer implements code following project standards
- Engineer commits changes when complete
- Return implementation completion status
```

### 3. Get Code Review from Tech Lead
```
Claude → Launch @agent-tech-lead-architect for code review:
- Review the committed code with high standards
- Provide specific feedback if changes needed
- Return review decision (approved/needs-changes)
```

### 4. Handle Review Feedback (if needed)
```
IF review shows issues:
Claude → Launch @agent-kotlin-java-engineer with feedback:
- Address specific review comments
- Commit fixes and return status
Claude → Repeat step 3 until approved
```

### 5. Update Documentation & Continue
```
Claude → Launch @agent-tech-lead-architect for documentation:
- Update project documentation for completed work
- Commit documentation changes
- Analyze project state and identify next priority task
- Return next task or signal no further tasks available
```

## Continuous Loop
Claude executes this loop until tech-lead-architect cannot find any further tasks:
```
REPEAT:
  1. Claude gets task from tech-lead-architect
  2. Claude assigns task to kotlin-java-engineer
  3. Claude gets code review from tech-lead-architect
  4. IF needs fixes: Claude sends feedback to engineer, GOTO 3
  5. Claude gets documentation update from tech-lead-architect  
  6. IF tech-lead-architect identifies more tasks: GOTO 1
  7. ELSE: Workflow complete (no further tasks available)
```

## Agent Responsibilities

### @agent-tech-lead-architect
- Follow responsibilities defined in agent configuration
- Focus on intention-based task assignment with research guidance

### @agent-kotlin-java-engineer  
- High-quality code implementation
- Comprehensive testing
- Documentation updates
- Code commits with clear messages
- Addressing review feedback

## Communication Rules
- Both agents can ask clarifying questions when requirements are unclear
- Follow communication guidelines defined in agent configurations
- Agents should confirm understanding before proceeding

## Termination Condition
Workflow continues until @agent-tech-lead-architect cannot identify any further tasks to assign. The tech lead will analyze the entire project state, test results, documentation, and roadmap to determine if there are any remaining tasks worth pursuing.

## Usage
Run this workflow to begin continuous development iterations between the tech lead and engineer agents.