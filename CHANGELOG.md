## 1.12.0
### Added
- Add run configuration creation support for ACM-liked challenges with test case as input.
 
## 1.11.0
### Added
- Add custom style support for challenge description rendering.
- Add go-back button in LuoGu solution webview.
 
## 1.10.2
### Fixed
- Resolve Cloudflare Turnstile verification issue on CodeForces submissions.
 
## 1.10.1
### Fixed
- Fix rendering issues in some LeetCode problem descriptions that use Markdown format.

## 1.10.0
### Added
- File names can be paths and support Velocity templates.
 
## 1.9.3
### Fixed
- Fix illegal characters in file name which come from some challenge titles   
 

## 1.9.1
### Fixed
- LeetCode: Use v2 api to query questions 

## 1.9.0
### Added
- HackerRank submission result support
- HackerRank user profile view support
### Fixed
- HackerRank login issue
 
## 1.8.0
### Added
- Add leetcode sql question support
 
## 1.7.4
### Fixed
- Fix luogu template document issue
 
## 1.7.3
### Fixed
- Fix luogu 2-factor-login issue
 
## 1.7.2
### Fixed
- Fix leetcode Golang question issue
 
## 1.7.0
### Added
- Add submission result view in embedded browser for AtCoder
### Fixed
- Automatically create missing directories when creating new code file
 
## 1.6.2
### Fixed
- Fix AtCode submission with cloudflare turnstile issue. [#62](https://github.com/WenjunHuang/CodeEpiphany/issues/62) 
 
## 1.6.1
### Fixed
- Fix proxy username and password not working issue
### Changed
- Optimize compatibility with JetBrains IDEs 2025.2
 
## 1.6.0
### Added
- Add submission result view in embedded browser for CodeForces, LuoGu
### Fixed
- Optimize api infrastructure

## 1.5.0
### Added
- Add user profile view for LeetCode, LeetCodeCN, LuoGu, CodeForces, AtCoder and HackerRank
- Add submission result view in embedded browser for LeetCode, LeetCodeCN
 
## 1.4.0
### Added
- Add plugin update notification
- Add plugin donation link
 
## 1.3.4

### Fixed
- Support LuoGu's netease yidun captcha code when submitting answer. fix [#60](https://github.com/WenjunHuang/CodeEpiphany/issues/60)
 
## 1.3.3

### Fixed
- LuoGu question query api changed, fix query issue
  
## 1.3.2

### Added

- Add LuoGu question solution

## 1.3.1

### Fixed

- Fix leetcode/leetcodecn test cases data not correctly parsed
  issue [#48](https://github.com/WenjunHuang/CodeEpiphany/issues/48)
- [#49](https://github.com/WenjunHuang/CodeEpiphany/issues/49)

## 1.3.0

### Added

- Add TestCase support for LeetCode, LeetCodeCN, LuoGu, CodeForces, AtCoder and HackerRank
- Add TestCase variable in code file template for LeetCode, LeetCodeCN, LuoGu, CodeForces, AtCoder and HackerRank

### Fixed

- [#16](https://github.com/WenjunHuang/CodeEpiphany/issues/36)
- [#29](https://github.com/WenjunHuang/CodeEpiphany/issues/29)
- [#37](https://github.com/WenjunHuang/CodeEpiphany/issues/37)
- [#38](https://github.com/WenjunHuang/CodeEpiphany/issues/38)
- [#46](https://github.com/WenjunHuang/CodeEpiphany/issues/46)

## 1.2.3

- Fix webview scrollbar style not compatible with IDE theme issue
- Fix user avatar icon size is too big for leetcode solution view in macos
- Fix codeforces code submission result issue

## 1.2.2

### Modified

- Fix chinese translation issues
- Modify readme and description documentation

## 1.2.0

### Added

- LeetCode and LeetCodeCN official solutions view support

### Modified

- Rename the old 'solutions' feature to 'my notes'

## 1.1.0

### Added

- Add [Browser Competitive-Companion](https://github.com/jmerle/competitive-companion) support
  to [CodeForces](https://codeforces.com/)

## 1.0.4

## Fixed

- Fix CodeForces login issue
- Fix code file template return empty string issue

## 1.0.2

### Changed

- Change leetcode icons

## 1.0.1

### Fixed

- Fix luogu user cookie expiration causing login failure
  issue.[#17](https://github.com/WenjunHuang/CodeEpiphany/issues/17)

### Changed

- Change description

## 1.0.0

### Added

- Automatically save search queries
- Save latest opened codedojo toolwindow and automatically open it when the IDE is restarted

## 0.9.8

### Changed

- Fix luogu login change

## 0.9.3

### Changed

- Fix leetcode company combobox dropdown slow

## 0.9.2

### Changed

- Fix a few bugs

## 0.9.0

### Added

- Add [LuoGu/洛谷](https://www.luogu.com.cn/) support

## 0.8.1

### Added

- Implement company question search for LeetCodeCN and LeetCode

## 0.8.0

### Changed

- Support Idea Platform from version 2023.3

## 0.7.1

### Added

- Implement [#5](https://github.com/WenjunHuang/CodeEpiphany/issues/5)

## 0.7.0

### Added

- Add [AtCoder](https://atcoder.jp) support

### Changed

- Fix [#10](https://github.com/WenjunHuang/CodeEpiphany/issues/10)
- Refine codeforces's language version
- Add 'view in browser' link for LeetCode, LeetCodeCN and HackerRank submissions
- Repair some UI problems

## 0.6.6

### Changed

- Fix the issue of missing text for action 'Surround Region'
- Fix leetcode result table display issue

## 0.6.5

### Added

- Implement [#6](https://github.com/WenjunHuang/CodeEpiphany/issues/6)
- Implement [#4](https://github.com/WenjunHuang/CodeEpiphany/issues/4)
- Add start and end markers for code regions to templates.

### Changed

- Offer simple templates for Code Forces challenges.
- Optimize some UI elements, including the difficulty color scheme and the login waiting indicator.

### Deprecated

### Removed

## 0.6.4

### Added

### Changed

- Fix [#3](https://github.com/WenjunHuang/CodeEpiphany/issues/3)
- Fix a database unique index constraint that prevented adding python and python3 simultaneously.
- Refactor code structure

### Deprecated

### Removed

## 0.6.3

### Added

### Changed

- optimize challenge submission result message
- Resolve the issue of missing language information when opening a challenge.
- fix [#2](https://github.com/WenjunHuang/CodeEpiphany/issues/2)

### Deprecated

### Removed

