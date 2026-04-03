# 🙂🥹 Mumble - Modern Chat Experience

Mumble is a premium, minimalist chat client built with Java. It features a sleek dark-themed UI, voice messaging, and a focus on visual excellence and smooth user experience.

![Chat UI Mockup](Screenshot.png)


## ✨ Key Features

-   **🎨 Premium Dark Mode**: A custom-designed dark theme using FlatLaf for a seamless, Discord-like aesthetic.
-   **🎤 Voice Messaging**: Integrated voice recorder and player powered by the Java Sound API.
-   **🖼️ Profile Avatars**: Support for user profile pictures and custom avatars.
-   **💬 Modern Messaging**: Refined chat bubbles with width constraints, smart grouping, and smooth vertical spacing.
-   **📌 Message Pinning**: Keep important messages at your fingertips (*Coming soon*).
-   **🛡️ Secure Communication**: (Add security details if applicable).


##

## 🚀 Getting Started

### Prerequisites

-   **Java 17** or higher
-   **Maven 3.6+**

### Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-username/Mumble.git
    cd Mumble
    ```

2.  **Install Dependencies**:
    ```bash
    mvn clean install
    ```

3.  **Run the Application**:
    ```bash
    mvn exec:java -Dexec.mainClass="com.mumble.gui.ChatUI"
    ```
    *Alternatively, use the provided `run.bat` for Windows.*

## 🛠️ Technology Stack

-   **Core**: Java 17
-   **UI Framework**: Swing with [FlatLaf](https://github.com/JFormDesigner/FlatLaf)
-   **Data Interchange**: [Gson](https://github.com/google/gson) for local JSON storage
-   **Icons**: [FontAwesome](https://fontawesome.com/) / Custom SVG icons

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

---
*Created with ❤️ by Soutik.*
