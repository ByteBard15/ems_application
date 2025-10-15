rootProject.name = "emp"

includeBuild("bootstrap")

include(
    ":auth",
    ":config",
    ":discovery",
    ":gateway",
    ":flyway",
    ":core:api",
    ":core:utils",
    ":core:messaging",
    ":employee",
)