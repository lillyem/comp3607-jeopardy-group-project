# COMP3607 JEOPARDY PROJECT GROUP 11

## Team Members

| Name | Student ID | Github Username |
|------|------------|-----------------|
| Daria Hypolite| 816042452 | dingding1707 |
| Sonali Maharaj | 816034459 | lillyem |
| Sonia Mohammed | 816040068 | soniarosem |

## Refer to the wiki for further documentation

# COMP3607 Jeopardy Game  
A Java-based Jeopardy-style game created for the COMP3607 Object Oriented Programming II course.  
The system supports multiple players, multiple file formats (CSV, JSON, XML), an interactive Swing GUI, scoring strategies, event logging, and summary report generation.

---

## Features

- Load question banks from **CSV**, **JSON**, or **XML**
- Supports **1–4 players** with custom names
- Fully interactive **Swing GUI**
- Dynamic Jeopardy board generation
- Question validation & duplicate prevention
- Standard scoring strategy (Strategy Pattern)
- Event logging for process mining
- Summary report generation
- Ability to **end game early**
- Ability to **start a new game** without restarting the program
- Javadoc API generated and hosted on GitHub Pages

---

## Project Structure

```text
src/main/java/com/jeopardy/model            # Core model classes (Question, Category, Player, etc.)
src/main/java/com/jeopardy/service          # Game logic, scoring strategies, data loaders
src/main/java/com/jeopardy/user_interface   # Swing GUI classes
src/test                                     # JUnit tests
docs/                                        # Generated Javadoc API (for GitHub Pages)
```

## Acknowledgements

- Created for the COMP3607 Software Engineering course.
- Built with Java, Maven, and Swing.
