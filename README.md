# Prism — Android Widget Platform

> An Android home-screen widget application focused on customizable, modern widget experiences.

**Status:** 🚧 Active Development

Prism is a Kotlin-based Android project for creating, previewing, and managing customizable home-screen widgets. The project uses a shared rendering pipeline so widget previews and rendered widgets stay consistent.

---

## ✨ Project Highlights

- Android home-screen widget system
- Customizable widget configurations
- Widget catalog and live editor
- Shared rendering pipeline
- Multiple widget families and variants
- Canvas-based rendering
- Bitmap-based widget output
- Modular Android architecture
- Automated testing and validation
- Documentation-driven development

---

## 📱 Widget System

A widget is represented as data rather than a fixed layout.

A `WidgetSpec` containing the widget family, variant, and user configuration is resolved into a `WidgetStyle`.

The rendering pipeline then converts the style into a bitmap that can be displayed through Android's widget system.

The same rendering path is used for previews and widget output, helping keep the editor preview consistent with the actual widget.

### Current project scale

- **32 widget families**
- **~13 variants per family**
- **~416 widget configurations**
- **~19 content renderers**
- **11 layouts**

---

## 🏗️ Architecture

The project follows a modular structure:

```text
app/
├── Main application module
│
├── core/
│   └── Shared application and rendering logic
│
├── feature/
│   └── Feature-specific functionality
│
├── widget/
│   └── Widget system and widget rendering
│
├── branding/
│   └── Application branding resources
│
└── docs/
    └── Architecture and development documentation
