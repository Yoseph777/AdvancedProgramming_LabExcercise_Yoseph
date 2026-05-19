# Lab Exercises Portfolio: Advanced Programming

Welcome to my Advanced Programming portfolio. This repository contains a series of four comprehensive lab exercises designed to demonstrate core concepts in Java based Fiel I/O, Database, Distributed Systems with RMI, JavaFX GUI, and real-time multi-threaded networking.

---

## Portfolio Overview

| Lab Exercise | Core Focus | Key Technologies |
| :--- | :--- | :--- |
| **Lab 1: Poker Game** | Desktop GUI & Game Logic | Java, JavaFX, Custom CSS |
| **Lab 2: Chat Application** | Multi-threaded Networking | Java Sockets, Threading, MySQL |
| **Lab 3: Notepad** | Local File I/O & Document Parsing | JavaFX, Java File I/O, PDF Engine |
| **Lab 4: Registry & Hub** | Full-Stack Architecture & RBAC | Java, WebSockets, MySQL |

---

## Detailed Lab Breakdowns

### Lab 1: Poker Game
A desktop-based multiplayer poker simulator featuring custom UI components, automated card dealing, and dynamic hand evaluation.

*   **Simultaneous Multi-Player Layout:** Implements a dynamic layout accommodating 3 players simultaneously, complete with custom CSS card styling and animations.
*   **Dynamic Hand Evaluation:** Features a robust engine that automatically evaluates and ranks complex poker hands (e.g., Straight, Flush, Full House) in real time.
*   **Tech Stack:** `Java` | `JavaFX` | `Custom CSS`

### Lab 2: Chat Application
A lightweight, multi-threaded networking application enabling seamless, real-time text communication between clients via a localized server environment.

*   **Concurrent Architecture:** Built on a multi-threaded server model capable of managing multiple active client connections without performance degradation.
*   **Connection Lifecycle:** Includes robust handlers for graceful client connection, unexpected dropouts, and clean disconnections.
*   **Broadcasting & Identity:** Features instant message broadcasting alongside custom user handles and identity persistence via an embedded data layer.
*   **Tech Stack:** `Java Sockets (ServerSocket)` | `Java Threading` | `MySQL`

### Lab 3: Notepad
A clean, functional text-editing desktop application focused on high-performance local storage management and native system file interaction.

*   **Document Operations:** Standard text manipulation tools supporting full CRUD workflows for native text files.
*   **OS Integration:** Deep integration with native OS file explorers for standard directory navigation, saving, and opening files.
*   **Extended Format Support:** Equipped with file parsing engines capable of opening and reading PDF documents directly within the UI.
*   **Tech Stack:** `JavaFX UI Framework` | `Java File I/O API`

### Lab 4: Integrated Registry & Communication Hub
An enterprise-style system designed to manage academic registries while simultaneously facilitating direct, secure communication channels between instructors and students.

*   **Role-Based Access Control (RBAC):** Implements strictly isolated and secure login workflows separating the Teacher and Student presentation layers.
*   **Academic Registry Engine:** Features comprehensive data management tools to execute full CRUD operations on student enrollments, grading structures, and academic profiles.
*   **Contextual Chat Rooms:** Seamlessly spins up private, WebSocket-driven communication spaces immediately upon authentication to bridge the gap between faculty and students.
*   **Tech Stack:** `Java` | `WebSockets` | `MySQL`

---
