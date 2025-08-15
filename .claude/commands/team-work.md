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
- @agent-taylor-lang-expert: Taylor language design compliance, language specification validation

Pass complete task specification to selected agent:
- Agent executes task following project standards
- Agent implements required changes
- Agent returns completion status and outcome details
```

### 4. Taylor Language Design Review (MANDATORY)
```
Claude → Launch @agent-taylor-lang-expert:
- Review implementations for Taylor language design compliance
- Ensure code aligns with Taylor's language principles and patterns
- Validate that test cases properly exercise Taylor language features
- Check consistency with Taylor language specifications
- Return APPROVE/REJECT with detailed feedback
- If REJECT: provide specific language design compliance requirements
```

### 5. Code Review (MANDATORY)
```
Claude → Launch @agent-code-reviewer:
- Review all code changes for quality, maintainability, and standards
- Assess adherence to project coding guidelines
- Verify implementation correctness and best practices
- Return APPROVE/REJECT with detailed feedback
- If REJECT: provide specific improvement requirements
```

### 6. Architecture Review (CONDITIONAL)
```
IF task involves system design, architecture, or significant structural changes:
  Claude → Launch @agent-system-design-architect:
  - Review architectural decisions and design patterns
  - Assess system boundaries and component interactions
  - Verify scalability and maintainability considerations
  - Return APPROVE/REJECT with architectural feedback
```

### 7. Document Knowledge (MANDATORY)
```
Each agent maintains their own documentation:
- Implementation agents update relevant docs with patterns discovered
- Code reviewer updates code guidelines with new rules
- System architect updates architecture guidelines with new patterns
- JVM expert updates JVM knowledge base with insights
- Taylor language expert updates language specification and design documentation
- All documentation must be concise and crisp, focused on intention over verbose details
```

### 8. Tech Lead Final Approval
```
Claude → Launch @agent-tech-lead with comprehensive outcome:
- Pass completed work details and status
- Include Taylor language design review results (APPROVE/REJECT + feedback)
- Include code review results (APPROVE/REJECT + feedback)
- Include architecture review results (if applicable)
- Include agent self-documentation status
- Tech lead makes final approval decision based on all reviews
- Tech lead determines if additional work needed or if task is complete
- Tech lead prepares for next iteration of workflow loop
```

### 9. Loop Continuation
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
  5. taylor_lang_review = review_taylor_language_design(outcome)
  6. code_review = review_code_with_code_reviewer(outcome)
  7. IF task_requires_architecture_review(task):
       architecture_review = review_architecture_with_architect(outcome)
  8. self_documentation = agents_maintain_own_docs(outcome, reviews)
  9. final_approval = get_tech_lead_approval(outcome, taylor_lang_review, code_review, architecture_review, self_documentation)
  10. IF final_approval == REJECT:
        CONTINUE  // Return to step 3 with feedback for rework
END WHILE

Project delivery complete - no remaining tasks
```

## Agent Responsibilities in Workflow

### @agent-tech-lead
- **Task Planning**: Analyze project state and identify next priority work
- **Agent Selection**: Recommend which agent should handle each task type
- **Final Approval**: Review all work including code review and architecture review results
- **Quality Gate**: Make final approval/rejection decisions based on comprehensive reviews
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

### @agent-taylor-lang-expert
- **Language Design Compliance**: Ensure all implementations align with Taylor language design
- **Specification Validation**: Verify code follows Taylor language principles and patterns
- **Test Case Review**: Validate that test cases properly exercise Taylor language features
- **Consistency Enforcement**: Check consistency with Taylor language specifications
- **Documentation Authority**: Own and maintain Taylor language design documentation

## Workflow Coordination Rules

### Task Handoff Protocol
1. Tech lead provides complete task specification
2. Claude selects most appropriate agent for task type
3. Agent receives full context and requirements
4. Agent executes task independently
5. Agent reports completion with detailed outcome
6. **MANDATORY**: Taylor language expert reviews for language design compliance
7. **MANDATORY**: Code reviewer reviews all code changes
8. **CONDITIONAL**: Architecture reviewer reviews structural changes (if applicable)
9. Each agent maintains their own documentation concisely
10. Tech lead makes final approval based on all review results
11. If approved: proceed to next task; if rejected: rework with feedback

### Quality Assurance
- Each agent maintains responsibility for quality within their domain
- **MANDATORY Taylor language design review** ensures all changes align with language specifications
- **MANDATORY code review** ensures all changes meet quality standards
- **CONDITIONAL architecture review** ensures structural integrity for significant changes
- **Tech lead final approval** acts as quality gate combining all review inputs
- **Rejection and rework process** ensures no substandard work proceeds
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