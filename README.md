# Media-To-ASCII Web Converter

A webapp that converts your images, videos, and GIFs into ASCII art! Upload any media file and let the program work its magic!

## Features

* Convert images (`.png`, `.jpg`, `.jpeg`), videos (`.mp4`, `.mov`, `.avi`, `.mkv`), and GIFs to ASCII art
* Adjust output width from 50 to 300 characters
* Choose from three character styles: Simple, Light Mode, or Dense
* Optional color output
* Optional dithering for enhanced detail
* Download individual frames as text files
* Export full videos with audio or GIFs
* Navigate frame-by-frame through videos and GIFs

## Requirements

* **Java:** 17 or higher (JDK must be installed)
* **FFmpeg:** Required for video/GIF processing (must be in system PATH or configured in `application.properties`)
* **Maven:** 3.6+ (for building the project)
* **Supported OS:** Windows, macOS, Linux

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/zappro74/Media-To-Ascii-Converter.git
cd Media-To-Ascii-Converter
```

### 2. Install FFmpeg

**Windows:**
```bash
# Using Chocolatey
choco install ffmpeg

# Or download from https://ffmpeg.org/download.html
# Add ffmpeg.exe to your system PATH
```

**macOS:**
```bash
brew install ffmpeg
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install ffmpeg

# Fedora
sudo dnf install ffmpeg
```

### 3. Configure FFmpeg Path (Optional)

If FFmpeg is not in your system PATH, edit `src/main/resources/application.properties`:

```properties
ffmpeg.executable=C:/path/to/ffmpeg.exe  # Windows
# or
ffmpeg.executable=/usr/local/bin/ffmpeg  # macOS/Linux
```

### 4. Build the Project

```bash
mvn clean install
```

## Running the Application

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

### Option 2: Using the JAR File

```bash
java -jar target/asciiweb-0.0.1-SNAPSHOT.jar
```

### Option 3: Using an IDE

1. Open the project in IntelliJ IDEA, Eclipse, or VS Code
2. Run the `AsciiwebApplication.java` class (located in `src/main/java/com/ascii/web/`)

## Usage

1. **Open your browser** and navigate to:
   ```
   http://localhost:8080
   ```

2. **Upload a file:**
   * Click "Choose File" and select an image, video, or GIF

3. **Configure options:**
   * **Ramp:** Choose character density (Simple, Light, Dense)
   * **Width:** Adjust ASCII width using the slider (50-300 characters)
   * **Color:** Enable colored ASCII output
   * **Dither:** Enable dithering for enhanced detail

4. **Convert:**
   * Click "Convert" to process your file
   * For videos/GIFs: Use Prev/Next buttons to navigate frames

5. **Download:**
   * **Current Frame:** Download the displayed frame as a `.txt` file
   * **Full Media:** Export the entire video as `.mp4` or GIF as `.gif`


#### Thanks for checking this out :)
###### As always, stay sassy
###### - PDP
