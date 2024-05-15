<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# vim-switch-ko Changelog

## [Unreleased]

## [0.0.5] - 2024-05-14

- Support to change to english when type `ctrl+[`
- Fix can not change locale in new terminal
Refactor compare current editor logic So, can work in the new terminal, git message editor and etc...

## [0.0.4] - 2024-05-09

- Fix typing issue on the sub input

## [0.0.3] - 2024-05-08

- Fix can not switch language issue in multi windows
The reason of issue occurred when checking current editor. In Multi windows situation the plugin check a first opening editor even if your focus has a second editor

## [0.0.2] - 2024-04-20

### Fixed

- Editor 가 아닌 다른 Panel 에서 한영 전환이 되지 않는 이슈를 수정, 반드시 Editor Panel 내에서만 적절히 한영 전환이 관리되도록 개선
- Editor 가 아닌 다른 Panel 에서 한글로 전환 후 Editor 로 돌아올 시, Editor 가 normal 일 경우 자동으로 영어로 전환하지 못해 VIM 노말모드 컨트롤이 불가능했던 이슈 수정

## [0.0.1] - 2024-04-02

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/owjs3901/vim-switch-ko/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/owjs3901/vim-switch-ko/commits/v0.0.1
