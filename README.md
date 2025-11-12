# Powder Sandbox (Kotlin JVM Edition)

A modern JVM-based edition of the Powder Sandbox, built in **Kotlin** for clarity, performance, and expressiveness. This version strikes a perfect balance between the **strong typing and functional features of Scala** and the **flexible scripting style of Groovy**, while maintaining a clean, modern syntax and high runtime efficiency.

Kotlin’s design makes this edition approachable for both JVM veterans and newcomers, offering the same core sandbox experience with a more concise and readable codebase.

### See [GameHub](https://github.com/RobertFlexx/Powder-Sandbox-GameHub) for more editions of this game.

---

## Features

* Built in **Kotlin** targeting the JVM
* Cross-platform support (Linux, macOS, Windows)
* Realistic physics: powders fall, liquids flow, gases rise, fire burns
* Electrical conduction with lightning, wire, and metal
* Dozens of elements with dynamic interactions
* AI-driven entities (humans and zombies)
* Fully colorized terminal UI with element browser
* Null safety and immutable data structures reduce runtime bugs
* Simple interop with Java and other JVM languages

---

## Requirements

* Java 17+ or any JDK compatible with Kotlin 1.9+
* Kotlin compiler (`kotlinc`) or IntelliJ IDEA for development

To install on Debian/Ubuntu:

```bash
sudo apt install openjdk-17-jdk kotlin
```

---

## Building and Running

Clone and enter the repository:

```bash
git clone https://github.com/RobertFlexx/Powder-Sandbox-Kotlin-Edition
cd Powder-Sandbox-Kotlin-Edition
```

Compile and run:

```bash
kotlinc PowderSandbox.kt -include-runtime -d powder.jar
java -jar powder.jar
```

Alternatively, if it’s a Gradle project:

```bash
gradle run
```

---

## Controls

| Key               | Action                 |
| ----------------- | ---------------------- |
| Arrow keys / WASD | Move cursor            |
| Space             | Place current element  |
| E                 | Erase with empty space |
| + / -             | Adjust brush size      |
| M / Tab           | Open element menu      |
| P                 | Pause simulation       |
| C / X             | Clear screen           |
| Q                 | Quit simulation        |

---

## Comparison: Kotlin vs [Scala Edition](https://github.com/RobertFlexx/Powder-Sandbox-Scala-Edition) vs [Groovy Edition](https://github.com/RobertFlexx/Powder-Sandbox-Groovy-Edition)

| Aspect            | Kotlin Edition (Modern JVM)                     | [Scala Edition](https://github.com/RobertFlexx/Powder-Sandbox-Scala-Edition) | [Groovy Edition](https://github.com/RobertFlexx/Powder-Sandbox-Groovy-Edition) |
| ----------------- | ----------------------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Language Style    | Modern, pragmatic, concise syntax               | Functional + OO hybrid, very expressive                                      | Dynamic and scripting-oriented                                                 |
| Type System       | Strong static typing with null safety           | Very advanced type system and pattern matching                               | Dynamically typed, runtime flexibility                                         |
| Performance       | Fast JVM JIT execution, near Java-level         | Similar JVM speed, slightly heavier on syntax                                | Slightly slower due to dynamic typing                                          |
| Readability       | Clean and intuitive                             | Powerful but verbose for newcomers                                           | Easiest for quick experimentation                                              |
| Safety            | Compile-time null safety and immutable defaults | Excellent with immutability and type checks                                  | Looser safety model (runtime errors possible)                                  |
| Interoperability  | Seamless with Java libraries and frameworks     | Great with Java, but can require more boilerplate                            | Excellent — can script directly with Java classes                              |
| Ease of Extension | Simple object + function expansion              | Advanced OOP/FP constructs enable complex extensions                         | Extremely easy — modify code live                                              |
| Ideal Use Case    | Balanced, modern JVM projects                   | Academic, functional, or high-abstraction simulations                        | Rapid prototyping and gameplay experiments                                     |

All three JVM editions share the same **simulation logic, element behavior, and terminal UI**, but their coding philosophies differ:

* **Kotlin** – clean and practical for long-term maintenance.
* **Scala** – expressive and advanced for abstract designs.
* **Groovy** – dynamic and easy for fast experimentation.

---

## License

Released under the BSD 3-Clause License.

---

## Author

**Robert (@RobertFlexx)**
Creator of FerriteOS, custom shells, editors, and many experimental terminal simulations.

GitHub: [https://github.com/RobertFlexx](https://github.com/RobertFlexx)
