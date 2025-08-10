# Two-Agent Development Workflow

## Overview
This command initiates a continuous development workflow between two specialized agents:
- **@agent-tech-lead-architect**: Project owner and task manager
- **@agent-kotlin-java-engineer**: Implementation engineer

## Workflow Process

### 1. Tech Lead Initialization
- **@agent-tech-lead-architect** reads README.md to understand current project status
- Reviews project documentation in docs/project/
- Conducts research (codebase analysis, industry best practices, language design patterns)
- De-ambiguates unclear tasks or project issues through analysis
- Identifies next priority task from available work

### 2. Task Assignment 
- **@agent-tech-lead-architect** creates a specific, scoped task for **@agent-kotlin-java-engineer**
- Task must be:
  - **Specific**: Clear scope and boundaries
  - **Straightforward**: No ambiguous requirements  
  - **Scoped**: Single feature/component focus
  - **Unambiguous**: Clear acceptance criteria

### 3. Implementation Phase
- **@agent-kotlin-java-engineer** receives task and starts implementation
- Writes high-quality Kotlin/Java code following project standards
- Includes comprehensive tests and documentation
- Commits changes when implementation is complete

### 4. Code Review Phase
- **@agent-tech-lead-architect** reviews the committed code
- Provides specific feedback if improvements needed
- **@agent-kotlin-java-engineer** addresses feedback and recommits
- Loop continues until code meets quality standards

### 5. Project Update Phase
- **@agent-tech-lead-architect** updates project documentation
- Updates task status in docs/project/tasks.md
- Updates project status in docs/project/index.md
- Commits documentation changes

### 6. Next Iteration
- **@agent-tech-lead-architect** identifies next priority task
- Process repeats from step 2

## Agent Responsibilities

### @agent-tech-lead-architect
- Project status analysis and planning
- Research (codebase, industry best practices, language design)
- Task breakdown and prioritization
- De-ambiguating unclear requirements or project issues
- Code review and quality assurance
- Project documentation maintenance (docs/project/ and docs/language/)
- Strategic decision making

### @agent-kotlin-java-engineer  
- High-quality code implementation
- Comprehensive testing
- Documentation updates
- Code commits with clear messages
- Addressing review feedback

## Communication Rules
- Both agents can ask clarifying questions when requirements are unclear
- All task assignments must include acceptance criteria
- All feedback must be specific and actionable
- Agents should confirm understanding before proceeding

## Termination Condition
Workflow continues until all tasks in docs/project/tasks.md are completed or project owner decides to stop.

## Usage
Run this workflow to begin continuous development iterations between the tech lead and engineer agents.