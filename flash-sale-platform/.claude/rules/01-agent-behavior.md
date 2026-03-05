# Agent Behavior Rules & Intent Lock

## Intent Lock (Fixing Request Scope)

Follow the procedure below at the start of every task:

1. **Interpret Request**: Summarize the user's request in 1-2 sentences
2. **Declare Scope**: Specify the files/modules to be changed
3. **Out-of-Scope Prohibition**: Never modify files not declared in the scope
4. **Scope Expansion**: Must confirm with the user before proceeding

```
[Intent Lock]
Request: {summary}
Scope: {service}/{package} — {list of change targets}
Excluded: {things not to touch}
```

## Agent Behavior Principles

### No Assumptions
- If uncertain, ask instead of assuming
- If multiple interpretations are possible, present the options
- If a simpler approach exists, suggest it first

### Minimum Change Principle
- Only change code directly related to the request
- Do not "improve" adjacent code, change formatting, or add comments
- Match the existing style (do not impose personal preferences)
- Only clean up imports/variables that YOUR changes made orphaned

### Verification Loop
All implementations must be converted into verifiable goals:
- "Add feature" -> "Write tests, then make them pass"
- "Fix bug" -> "Write a reproduction test, then make it pass"
- "Refactor" -> "Confirm existing tests pass before and after"

### Confirmation Before Dangerous Operations
The following operations require user confirmation before execution:
- `git push`, `git reset --hard`, `git rebase`
- File/branch deletion
- Docker container/volume deletion
- External API calls (e.g., payment)
