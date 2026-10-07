package com.rexaps.librex.rexcode

enum class RexLanguage(val id: String, val displayName: String) {
    JAVASCRIPT("javascript", "JavaScript"), TYPESCRIPT("typescript", "TypeScript"),
    JAVA("java", "Java"), KOTLIN("kotlin", "Kotlin"), PYTHON("python", "Python"),
    HTML("html", "HTML"), CSS("css", "CSS"), JSON("json", "JSON"), XML("xml", "XML"),
    C("c", "C"), CPP("cpp", "C++"), CSHARP("csharp", "C#"), GO("go", "Go"),
    RUST("rust", "Rust"), PHP("php", "PHP"), SQL("sql", "SQL"), SHELL("shell", "Shell"),
    YAML("yaml", "YAML"), MARKDOWN("markdown", "Markdown"), DART("dart", "Dart"),
    SWIFT("swift", "Swift"), RUBY("ruby", "Ruby"), JSX("jsx", "JSX"), TSX("tsx", "TSX"),
    VUE("vue", "Vue"), GRADLE("gradle", "Gradle"), KTS("kotlin-script", "Kotlin Script"),
    TOML("toml", "TOML"), ENV("env", "Environment"), GITIGNORE("gitignore", "Git Ignore"),
    DOCKERFILE("dockerfile", "Dockerfile"), BATCH("batch", "Batch"), R("r", "R"), LUA("lua", "Lua"),
    SCSS("scss", "SCSS"), SASS("sass", "Sass"), LESS("less", "Less"), INI("ini", "INI"), TEXT("text", "Plain Text")
}
