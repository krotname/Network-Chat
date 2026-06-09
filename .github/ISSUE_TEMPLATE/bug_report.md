name: Bug report
description: Report a defect
title: "[Bug] "
labels: ["bug"]
body:
  - type: textarea
    id: reproduction
    attributes:
      label: Reproduction
      description: Steps to reproduce
  - type: input
    id: version
    attributes:
      label: Version
      description: App/Java/Gradle version
  - type: textarea
    id: log
    attributes:
      label: Logs
      description: Paste relevant logs
