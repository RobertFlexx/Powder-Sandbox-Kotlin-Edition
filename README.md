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
* **JNA (Java Native Access)** library for ncurses terminal rendering

To install on Debian/Ubuntu:

```bash
sudo apt install openjdk-17-jdk kotlin libjna-java
```

To install on Fedora/Ultramarine/Chimera:

```bash
sudo dnf install java-17-openjdk kotlin jna jna-platform
```

---

## Building and Running

Clone and enter the repository:

```bash
git clone https://github.com/RobertFlexx/Powder-Sandbox-Kotlin-Edition
cd Powder-Sandbox-Kotlin-Edition
```

### Option 1 – Compile Manually with `kotlinc`

Compile using the JNA libraries already installed on your system:

```bash
kotlinc PowderSandbox.kt \
  -cp "/usr/share/java/jna.jar:/usr/share/java/jna-platform.jar" \
  -include-runtime -d powder.jar
```

Then run:

```bash
java -cp "powder.jar:/usr/share/java/jna.jar:/usr/share/java/jna-platform.jar" PowderSandboxKt
```

If you don’t have `jna-platform.jar`, install it via your package manager or manually download it:

```bash
sudo wget -P /usr/share/java https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.14.0/jna-platform-5.14.0.jar
```

### Option 2 – Use Gradle (Recommended)

Create a `build.gradle.kts` file:

```kotlin
plugins {
    kotlin("jvm") version "2.0.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

application {
    mainClass.set("PowderSandboxKt")
}
```

Then just run:

```bash
gradle run
```

This setup automatically downloads JNA and compiles everything properly.

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

## Troubleshooting

**Problem:** `unresolved reference: jna`

**Solution:** Ensure the JNA JARs exist and are included in your classpath:

```bash
sudo find /usr -name "jna*.jar"
```

Then use those paths in your `-cp` argument.

**Problem:** `ncurses` not working on Windows.

**Solution:** JNA ncurses bindings are *natively Unix-only*. For Windows, use **Windows Terminal** under WSL2 or port to `jna-win32-console`.

**Problem:** Flickering or color issues.

**Solution:** Ensure your terminal supports 256-color mode and UTF-8:

```bash
export TERM=xterm-256color
export LANG=en_US.UTF-8
```

---

## License

Released under the BSD 3-Clause License.

---

## Author

**Robert (@RobertFlexx)**
Creator of FerriteOS, custom shells, editors, and many experimental terminal simulations.

GitHub: [https://github.com/RobertFlexx](https://github.com/RobertFlexx)
