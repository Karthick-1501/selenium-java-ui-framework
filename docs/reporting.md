# Reporting

Problem:
Duplicate test nodes in parallel execution

Solution:
ConcurrentHashMap + computeIfAbsent

Result:
Single parent per class, clean hierarchy.
