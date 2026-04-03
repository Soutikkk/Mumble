# 🙂 Mumble - Modern Realtime Chat Application Experience

Mumble is a premium, minimalist chat client built with Java. It features a sleek dark-themed UI, voice messaging, and a focus on visual excellence and smooth user experience.

<img width="1289" height="862" alt="screenshot" src="https://github.com/user-attachments/assets/0cecd26a-bbde-41eb-90ed-2224f7f25aa8" />


## ✨ Key Features

- 🎨 **Premium Dark Mode**: Custom-designed black theme with layered colors for a modern, Discord-like UI.
- 💬 **Modern Messaging UI**: Clean chat bubbles with width constraints, proper spacing, and message grouping.
- ⚡ **Real-Time Messaging**: Instant message delivery using Java sockets with multithreading support.
- 👤 **Private Chat (1-to-1)**: Secure direct messaging between users with dedicated chat windows.
- 👥 **Group Chat System**: Create and participate in group conversations with real-time broadcasting.
- 🔁 **Reply to Messages**: Reply to specific messages with contextual preview inside chat bubbles.
- 😀 **Emoji Support**: Integrated emoji picker with proper rendering inside messages.
- 🎤 **Voice Messaging**: Record and send audio messages using Java Sound API with playback support.
- 📌 **Message Pinning**: Pin important messages and access them easily at the top of chat.
- 🗑️ **Message Deletion**: Delete sent messages with instant UI updates.
- ✔️ **Message Status Indicators**: Visual feedback for sent (✓) and delivered (✓✓) messages.
- 👤 **Profile Avatars**: Support for user profile pictures and dynamic avatar display.
- 🟢 **User Status Indicators**: Online/Offline presence shown in sidebar.
- 🔍 **User Search**: Quickly find users and initiate private chats.
- 🧭 **Smart Sidebar Navigation**: Organized user/group list with active chat highlighting.
- 🔄 **Multi-Client Support**: Multiple users connected simultaneously with real-time sync.
- 🧠 **OOP-Based Architecture**: Clean modular design using object-oriented principles.
- 🌐 **Client-Server Architecture**: Centralized server handles connections and message routing.
- ⚙️ **Efficient Networking**: Built using Java Socket Programming with concurrent handling.
- ⌨️ **Smart Input System**: Supports text, emoji, and quick message sending.
- 🔒 **Secure Communication (Basic)**: Structured messaging system with scope for encryption.


##

## 🚀 Getting Started

### Prerequisites

-   **Java 17** or higher
-   **Maven 3.6+**

### Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/Soutikkk/Mumble.git
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

This project is licensed under the MIT License - see the [LICENSE]
(LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

---
- *(Made this for College Project...)*
- *Created with ❤️ by Soutik.*
