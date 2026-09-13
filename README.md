# Concurrent AI Agent Orchestration Gateway

A Spring Boot project for building an AI-powered document generation system while exploring real-world software engineering concepts such as:

- Low-Level Design (LLD)
- Polymorphism and dependency injection
- AI/LLM provider abstraction
- Agent orchestration
- Concurrent execution
- ExecutorService
- Futures
- Timeouts
- Cancellation
- Thread interruption
- Bounded concurrency
- Dependency-driven workflows

The project is intentionally being built incrementally.

The goal is not to start with a large architecture and implement it blindly.

Instead:

> Build → encounter a problem → understand the problem → redesign → measure

---

# 1. Current Goal

The final product we are evolving toward is an **AI document generation agent**.

Given a user prompt such as:

```text
Explain Java virtual threads in 3 sentences.
```

the system should eventually:

```text
User Prompt
     ↓
Research
     ↓
Analysis
     ↓
Documentation
     ↓
Clean Document
```

The important part is that each stage performs meaningful work rather than simply calling an LLM.

---

# 2. Current Architecture

The application currently has this structure:

```text
HTTP Request
    ↓
AgentController
    ↓
AgentService
    ↓
AgentOrchestrator
    ↓
ResearchAgent
    ├── TopicOverviewTask
    ├── TechnicalDetailsTask
    ├── ExamplesTask
    └── LimitationsTask
    ↓
AnalysisAgent
    ↓
DocumentationAgent
    ↓
HTTP Response
```

At the moment, the three main agents execute sequentially:

```text
Research
   ↓
Analysis
   ↓
Documentation
```

Inside `ResearchAgent`, independent research tasks execute concurrently.

---

# 3. V1 — Simple LLM Agent

The project initially started with a very simple flow:

```text
POST /agent/run
     ↓
AgentService
     ↓
ChatClient
     ↓
Gemini
     ↓
AgentResponse
```

The request looked like:

```json
{
  "prompt": "Explain Java virtual threads"
}
```

The response was:

```json
{
  "answer": "..."
}
```

This established that the Spring Boot + Spring AI + Gemini setup worked.

---

# 4. LLM Provider Abstraction

The first real LLD problem was:

> What happens if we don't want our entire application to depend directly on Gemini?

We introduced:

```java
public interface LLMProvider {

    String generate(String prompt);
}
```

The concrete implementation:

```text
LLMProvider
    │
    ├── GeminiProvider
    └── MockLLMProvider
```

`GeminiProvider` uses Spring AI's `ChatClient`.

`MockLLMProvider` allows the application to run without making a real LLM request.

This allows the implementation to be switched without changing the consumers.

For example:

```java
@Primary
@Component
public class GeminiProvider implements LLMProvider {
    ...
}
```

or:

```java
@Primary
@Component
public class MockLLMProvider implements LLMProvider {
    ...
}
```

### Design lesson

The abstraction exists because there are **interchangeable implementations**:

```text
Gemini OR OpenAI OR Anthropic OR Mock
```

It wasn't introduced merely because "interfaces are good."

---

# 5. AgentTask Abstraction

The next requirement was to have multiple agents.

We introduced:

```java
public interface AgentTask {

    String name();

    String execute(String prompt);
}
```

Concrete agents:

```text
AgentTask
    │
    ├── ResearchAgent
    ├── AnalysisAgent
    └── DocumentationAgent
```

Spring injects all implementations:

```java
private final List<AgentTask> agents;
```

The orchestrator executes them:

```java
for (AgentTask agent : agents) {
    String response = agent.execute(prompt);
    ...
}
```

`name()` was added because otherwise the orchestrator would lose the identity of the result-producing agent.

---

# 6. Initial Sequential Execution

Initially, `ResearchAgent` was itself a single operation.

The project was deliberately made slow using artificial delays.

Example:

```java
Thread.sleep(1000);
```

We measured the complete pipeline and observed approximately:

```text
Research
   ↓
Analysis
   ↓
Documentation

Total ≈ 7 seconds
```

This led to the next question:

> Can some of the work happen concurrently?

---

# 7. Dependency Analysis

Before introducing concurrency, we identified an important distinction.

Not every agent can execute concurrently.

For example:

```text
Research
   ↓
Analysis
   ↓
Documentation
```

Analysis needs research.

Documentation needs analysis.

Therefore:

```text
Research → Analysis → Documentation
```

is a dependency chain.

However, research itself can be decomposed:

```text
ResearchAgent
    │
    ├── TopicOverviewTask
    ├── TechnicalDetailsTask
    ├── ExamplesTask
    └── LimitationsTask
```

These tasks are currently independent.

Therefore:

```text
TopicOverviewTask ───────┐
TechnicalDetailsTask ────┤
ExamplesTask ─────────────┼──→ Combined Research
LimitationsTask ─────────┘
```

is a good concurrency boundary.

---

# 8. ResearchTask Abstraction

We introduced:

```java
public interface ResearchTask {

    String name();

    String execute(String prompt);
}
```

Concrete implementations:

```text
ResearchTask
    │
    ├── TopicOverviewTask
    ├── TechnicalDetailsTask
    ├── ExamplesTask
    └── LimitationsTask
```

Each task represents one independent piece of research.

For example:

```java
@Component
public class TopicOverviewTask implements ResearchTask {

    @Override
    public String name() {
        return "TopicOverviewTask";
    }

    @Override
    public String execute(String prompt) {
        ...
    }
}
```

The tasks initially used artificial delays to make concurrency measurable.

---

# 9. ResearchAgent Becomes an Internal Orchestrator

`ResearchAgent` was changed from:

```text
ResearchAgent
     ↓
one operation
```

to:

```text
ResearchAgent
     ↓
List<ResearchTask>
     ↓
execute each task
     ↓
combine results
```

Conceptually:

```java
private final List<ResearchTask> researchTasks;
```

Spring injects all `ResearchTask` implementations.

Initially, they were executed sequentially:

```text
Task 1 → Task 2 → Task 3 → Task 4
```

With four tasks taking approximately 500 ms each:

```text
500 + 500 + 500 + 500
≈ 2000 ms
```

---

# 10. Introducing Concurrency

We then introduced:

```java
ExecutorService
```

The tasks are submitted:

```java
Future<String> future =
    executorService.submit(() -> {
        String response = task.execute(prompt);
        return task.name() + ": " + response;
    });
```

All research tasks are submitted before results are collected.

Conceptually:

```text
                 ExecutorService
                 /      |      \
                /       |       \
Task 1 ────────→        │        \
Task 2 ────────────────→         \
Task 3 ─────────────────────────→
Task 4 ─────────────────────────→
```

Then:

```java
future.get(...)
```

is used to collect the results.

---

# 11. Why `Future`?

A `Future` represents a computation that has been submitted but may finish later.

Instead of:

```text
execute task
wait
execute task
wait
execute task
wait
```

we can:

```text
submit task 1 → Future
submit task 2 → Future
submit task 3 → Future
submit task 4 → Future

then collect results
```

This allowed independent research tasks to execute concurrently.

---

# 12. Measured Improvement

Before concurrency:

```text
ResearchAgent ≈ 7 seconds
```

After concurrent research tasks:

```text
ResearchAgent ≈ 2 seconds
```

The reason is:

Sequential:

```text
Task A = 500ms
Task B = 500ms
Task C = 500ms
Task D = 500ms

Total ≈ 2000ms
```

Concurrent:

```text
Task A ─┐
Task B ─┤
Task C ─┼──→ ≈ 500ms
Task D ─┘
```

The research phase is approximately bounded by the slowest task rather than the sum of all task durations.

---

# 13. Executor Lifecycle Problem

The first concurrent implementation created an executor inside every request:

```java
ExecutorService executor =
    Executors.newFixedThreadPool(researchTasks.size());
```

This was identified as a problem.

If 100 requests arrive:

```text
100 requests
    ×
4 threads per request
    =
potentially 400 threads
```

This doesn't scale well.

Therefore the executor was moved to application-level dependency management.

The architecture became:

```text
Spring Application
      │
      ├── ResearchAgent
      │
      └── Research Executor
               │
               ├── Worker
               ├── Worker
               ├── Worker
               └── Worker
```

The executor is now shared instead of being created for every request.

---

# 14. Bounded Concurrency

The executor currently uses a fixed number of workers:

```java
Executors.newFixedThreadPool(4)
```

The important idea is:

> The number of incoming requests should not directly determine the number of threads created.

For example:

```text
Request A ─┐
Request B ─┤
Request C ─┼──→ shared bounded executor
Request D ─┤
Request E ─┘
```

If all workers are busy, additional work waits for an available worker.

This introduces the concept of **bounded concurrency**.

---

# 15. Timeouts

Another real concurrency problem appeared:

> What happens if an individual research task takes too long?

We added:

```java
future.get(2, TimeUnit.SECONDS);
```

This means:

> Wait at most 2 seconds for this Future when collecting its result.

If the timeout occurs:

```java
catch (TimeoutException e) {
    future.cancel(true);
}
```

The task is cancelled and the executing thread is interrupted.

---

# 16. Cancellation and Interruption

We tested cancellation using:

```java
Thread.sleep(10000);
```

The task was intentionally made to run for 10 seconds.

After approximately 2 seconds:

```text
Future timeout
      ↓
future.cancel(true)
      ↓
worker thread interrupted
      ↓
InterruptedException
```

The console confirmed:

```text
LimitationsTask STARTED
LimitationsTask INTERRUPTED
```

and did not print:

```text
LimitationsTask FINISHED
```

This experimentally demonstrated that:

```java
future.cancel(true);
```

requests interruption of the executing task.

However:

> Interruption is cooperative.

Java does not forcibly kill an arbitrary thread.

A task must respond appropriately to interruption.

---

# 17. Current Timing Experiment

We tested:

```text
TopicOverviewTask       = 500ms
TechnicalDetailsTask    = 2000ms
ExamplesTask            = 500ms
LimitationsTask         = 10000ms
```

The research timeout was:

```text
2 seconds
```

Observed:

```text
ResearchAgent      ≈ 2011 ms
AnalysisAgent      ≈ 3005 ms
DocumentationAgent ≈ 2003 ms
```

Total:

```text
≈ 7 seconds
```

This was expected.

The research stage was successfully bounded at approximately 2 seconds, while the remaining pipeline stages still took their own time.

---

# 18. Current Agent Pipeline

The measured pipeline is:

```text
ResearchAgent
    │
    │ ≈ 2 sec
    ↓
AnalysisAgent
    │
    │ ≈ 3 sec
    ↓
DocumentationAgent
    │
    │ ≈ 2 sec
    ↓
Response
```

This is intentionally sequential because these stages represent dependencies.

---

# 19. Current Code Structure

Conceptually:

```text
backend.orchestra
│
├── controller
│   └── AgentController
│
├── service
│   └── AgentService
│
├── agents
│   ├── AgentTask
│   ├── ResearchAgent
│   ├── AnalysisAgent
│   └── DocumentationAgent
│
├── tasks
│   └── research
│       ├── ResearchTask
│       ├── TopicOverviewTask
│       ├── TechnicalDetailsTask
│       ├── ExamplesTask
│       └── LimitationsTask
│
└── config
    └── ExecutorConfig
```

---

# 20. Current ResearchAgent Responsibilities

`ResearchAgent` currently does three things:

1. Receives the research tasks.
2. Executes independent research tasks concurrently.
3. Combines their results.

Conceptually:

```text
ResearchAgent
      │
      ├── submit Task 1
      ├── submit Task 2
      ├── submit Task 3
      └── submit Task 4
              ↓
          Future<String>
              ↓
        collect results
              ↓
       combined research
```

---

# 21. Current Important Design Decisions

### LLMProvider

Used because LLM providers are interchangeable:

```text
LLMProvider
    ├── Gemini
    ├── Mock
    └── future providers
```

### AgentTask

Used to give different agents a common executable contract:

```text
Research
Analysis
Documentation
```

### ResearchTask

Used because research can be decomposed into independent pieces:

```text
Overview
Technical Details
Examples
Limitations
```

### ExecutorService

Used because independent research tasks can execute concurrently.

### Future

Used to represent asynchronous task results and wait for them.

### Timeout

Used to prevent a slow research task from blocking indefinitely.

### Cancellation

Used when a task exceeds its allowed waiting/execution budget.

---

# 22. What We Have NOT Done Yet

Several important design problems remain intentionally unresolved.

## Agent Data Flow

Currently:

```text
ResearchAgent
      ↓
AnalysisAgent(prompt)
      ↓
DocumentationAgent(prompt)
```

But the desired system needs:

```text
ResearchAgent
      ↓
research result
      ↓
AnalysisAgent(prompt + research)
      ↓
analysis result
      ↓
DocumentationAgent(prompt + research + analysis)
```

The current:

```java
String execute(String prompt);
```

contract does not represent this data flow well.

This is the **next LLD problem to solve**.

---

# 23. Future Problems to Explore

After fixing agent data flow, the project can naturally evolve into:

```text
Agent workflow / DAG
        ↓
Structured execution context
        ↓
Better result representation
        ↓
Partial failures
        ↓
Timeout policies
        ↓
Cancellation propagation
        ↓
Retries
        ↓
Bounded concurrency
        ↓
Backpressure
        ↓
Virtual threads
        ↓
CompletableFuture
        ↓
Structured concurrency
```

The important rule is:

> Do not add these because they are "modern Java features."

Add them only when the requirements create the corresponding problem.

---

# 24. Learning Approach

This project follows:

```text
BUILD
  ↓
REDUCE
  ↓
REFRAME
  ↓
INCUBATE
  ↓
repeat
```

### BUILD

Implement the simplest real version.

### REDUCE

Reduce the problem to a precise engineering question.

Example:

> Four independent tasks currently execute sequentially.

### REFRAME

Identify the underlying principle:

> Independent work can execute concurrently.

### INCUBATE

Step away and think about the consequences before adding another abstraction.

---

# 25. Current State

The project currently has:

- Spring Boot application
- REST endpoint
- Gemini integration
- LLM provider abstraction
- Mock LLM provider
- Agent abstraction
- Agent orchestrator
- Research task abstraction
- Multiple research tasks
- Concurrent research execution
- Shared bounded executor
- Futures
- Per-task timeout
- Cancellation
- Thread interruption handling
- Measured concurrency improvements

The next major problem is:

```text
How should output from one agent become input to the next agent?
```

Current desired flow:

```text
User Prompt
     ↓
ResearchAgent
     ↓
Research Result
     ↓
AnalysisAgent
     ↓
Analysis Result
     ↓
DocumentationAgent
     ↓
Generated Document
```

The architecture should evolve from this requirement rather than introducing a pre-designed abstraction prematurely.
