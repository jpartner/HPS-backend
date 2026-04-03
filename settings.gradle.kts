rootProject.name = "hps-system"

includeBuild("build-logic")

include(
    "hps-common",
    "hps-domain",
    "hps-persistence",
    "hps-search",
    "hps-geo",
    "hps-messaging",
    "hps-api",
    "hps-app"
)
