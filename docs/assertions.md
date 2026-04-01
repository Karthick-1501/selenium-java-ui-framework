# Assertion Engine

Problem:
UI shows formatted numbers (1.00), Java reads 1.0

Solution:
BigDecimal comparison using compareTo()

Prevents false negatives in financial validations.
