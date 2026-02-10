# Sorting Hat App

A desktop Java Swing quiz that sorts the user into a Hogwarts house and displays house probabilities.

## Features
- Three quiz lengths: **Quick**, **Standard**, **Thorough**
- Likert-scale questions plus optional forced-choice tie-break questions
- Loads question banks and images from the `resources/` folder at runtime
- Uses **Gson** to parse JSON item banks

## Folder Structure
```
SortingHatApp.java
controller/
  MainController.java
model/
  ItemBank.java
  ScoringEngine.java
  SortingHatModel.java
  ResponseTimeWeight.java
view/
  MainWindow.java
  ParchmentBackgroundPanel.java
  Screens.java
  SortingHatTheme.java
  UiComponents.java
config/
  Config.java
resources/
  LikertItems.json
  ForcedChoiceItems.json
  TraitToHouseWeights.json
  HatIcon.png
  ParchmentBackground.png
  QuickIcon.png
  StandardIcon.png
  ThoroughIcon.png
libs/
  gson-2.10.1.jar
```

## Requirements
- **Java 11+**
- `libs/gson-2.10.1.jar` (included in this project)

## Build & run (terminal)

**Important:** This app loads JSON and images from the classpath at paths like `/resources/LikertItems.json`.  
So, when running from the terminal, make sure your **classpath includes the project root** (`.`), so the `resources/` folder is found.

### Windows (PowerShell or Command Prompt)

From the **project root**:

```bat
mkdir build

javac -cp "libs/gson-2.10.1.jar" -d build SortingHatApp.java controller\*.java model\*.java view\*.java config\*.java

java -cp "build;libs/gson-2.10.1.jar;." SortingHatApp
```

### macOS / Linux (Terminal)

From the **project root**:

```bash
mkdir -p build

javac -cp "libs/gson-2.10.1.jar" -d build SortingHatApp.java controller/*.java model/*.java view/*.java config/*.java

java -cp "build:libs/gson-2.10.1.jar:." SortingHatApp
```

### Common troubleshooting

- If you see an error like `Missing classpath resource: /resources/...`:
  - You likely forgot `;.` (Windows) or `:.` (macOS/Linux) in the **java** command.
  - You must run from the project root (where the `resources/` folder lives).

## References
OpenAI (2025) ChatGPT (Version 5.1) [Large language model]. Available at: https://chat.openai.com/  
(Accessed: 5 December 2025).

International Personality Item Pool (n.d.) IPIP: International Personality Item Pool. Available at: https://ipip.ori.org/index.htm  
(Accessed: 5 December 2025).  

## AI Use Statement
Everything inside the `resources/` directory was created with help of ChatGPT (OpenAI, 2025), including:
- JSON files: `LikertItems.json`, `ForcedChoiceItems.json`, `TraitToHouseWeights.json`
- Image files: `HatIcon.png`, `ParchmentBackground.png`, `QuickIcon.png`, `StandardIcon.png`, `ThoroughIcon.png`

ChatGPT (OpenAI, 2025) was used to generate initial Likert-scale items. During this process, the model indicated that it had internally used material from the International Personality Item Pool (IPIP).

## Disclaimer
This is an unofficial fan/class project and is not affiliated with or endorsed by J.K. Rowling, Warner Bros., or any related rights holders.
