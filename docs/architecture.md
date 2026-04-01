# Architecture

Framework follows layered design:
- Base → lifecycle
- Driver → ThreadLocal management
- Pages → business abstraction
- Utils → reusable logic

Why?
To ensure separation of concerns and scalability.
