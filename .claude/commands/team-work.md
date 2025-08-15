# Team Work Workflow

## Overview
This command implements a continuous team workflow that is key to delivering the project successfully. The workflow orchestrates all agents under `.claude/agents/` in an infinite loop until the project is fully implemented and no further tasks remain.

## Workflow Process

The team workflow operates as an infinite loop with the following steps:

### 1. Get Next Task from Tech Lead
```
Claude → Launch @agent-tech-lead:
- Analyze current project state and progress
- Identify next priority task based on project needs
- Create specific task assignment with clear requirements
- Return structured task specification OR indicate no tasks remain
```

### 2. Task Assignment Decision
```
IF @agent-tech-lead returns no task:
  STOP workflow loop - project is complete
  
ELSE IF task is returned:
  Proceed to step 3 with the assigned task
```

### 3. Execute Task with Appropriate Agent
```
Claude → Determine agent based on task type and launch appropriate agent:

- @agent-senior-code-engineer: Feature implementation, complex bug fixes, refactoring
- @agent-code-reviewer: Code quality assessment, review processes, guideline enforcement  
- @agent-system-design-architect: Architecture decisions, design patterns, system boundaries
- @agent-jvm-bytecode-expert: JVM issues, bytecode analysis, verification errors

Pass complete task specification to selected agent:
- Agent executes task following project standards
- Agent implements required changes
- Agent returns completion status and outcome details
```

### 4. Document Knowledge (MANDATORY)
```
Claude → Launch @agent-knowledge-curator:
- Extract insights from completed task
- Document patterns, solutions, and learnings
- Update appropriate knowledge repositories
- Ensure knowledge is properly indexed
```

### 5. Report Back to Tech Lead
```
Claude → Launch @agent-tech-lead with task outcome:
- Pass completed work details and status
- Include knowledge documentation status
- Allow tech lead to assess completion quality
- Tech lead determines if additional work needed on this task
- Tech lead prepares for next iteration of workflow loop
```

### 6. Loop Continuation
```
REPEAT from step 1:
Continue until @agent-tech-lead indicates no further tasks available
```

## Infinite Loop Logic
```
WHILE (project not complete):
  1. task = get_next_task_from_tech_lead()
  2. IF task == NO_TASK:
       BREAK  // Project fully implemented
  3. agent = determine_appropriate_agent(task)
  4. outcome = execute_task_with_agent(agent, task)
  5. knowledge = document_learnings_with_knowledge_curator(outcome)
  6. report_outcome_to_tech_lead(outcome, knowledge)
END WHILE

Project delivery complete - no remaining tasks
```

## Agent Responsibilities in Workflow

### @agent-tech-lead
- **Task Planning**: Analyze project state and identify next priority work
- **Agent Selection**: Recommend which agent should handle each task type
- **Progress Tracking**: Monitor completion and decide on next steps
- **Project Oversight**: Determine when project is fully implemented
- **Termination Decision**: Signal when no further tasks remain

### @agent-senior-code-engineer
- **Implementation**: Execute complex features and significant code changes
- **Quality Code**: Deliver maintainable, well-structured implementations
- **Testing**: Ensure proper test coverage for new functionality
- **Documentation**: Update relevant code documentation

### @agent-code-reviewer
- **Quality Assurance**: Review code for maintainability and standards compliance
- **Guideline Enforcement**: Ensure adherence to project coding standards
- **Process Improvement**: Identify opportunities for better development practices

### @agent-system-design-architect
- **Architecture Design**: Make high-level system design decisions
- **Pattern Application**: Apply appropriate design patterns and principles
- **System Boundaries**: Define clear module and component boundaries
- **Scalability Planning**: Design for future growth and maintenance

### @agent-jvm-bytecode-expert
- **JVM Analysis**: Handle bytecode-level debugging and optimization
- **Verification Issues**: Resolve JVM verification errors and class loading problems
- **Performance Analysis**: Optimize JVM-specific performance bottlenecks

## Workflow Coordination Rules

### Task Handoff Protocol
1. Tech lead provides complete task specification
2. Claude selects most appropriate agent for task type
3. Agent receives full context and requirements
4. Agent executes task independently
5. Agent reports completion with detailed outcome
6. Tech lead assesses results and plans next task

### Quality Assurance
- Each agent maintains responsibility for quality within their domain
- Tech lead coordinates overall project quality and completeness
- Workflow continues until all project requirements satisfied

### Communication Standards
- All task assignments include clear success criteria
- Agents report both successful completion and any blockers encountered
- Tech lead maintains project-wide context and priorities

## Termination Condition
The workflow loop terminates when @agent-tech-lead determines:
- All project features are fully implemented
- All bugs and issues are resolved
- All documentation is complete and up-to-date
- No further tasks can be identified that add value to the project
- Project is ready for delivery

## Usage
Run this command to begin the continuous team workflow that will drive the project to successful completion through coordinated agent collaboration.

## Key Success Factors
- **Infinite Loop Design**: Ensures work continues until truly complete
- **Tech Lead Oversight**: Central coordination prevents gaps or conflicts
- **Appropriate Agent Selection**: Right skills applied to each task type
- **Outcome Reporting**: Continuous feedback loop for project progress
- **Clear Termination**: Explicit completion criteria prevent endless loops