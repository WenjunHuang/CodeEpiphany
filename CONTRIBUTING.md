# Contributing to CodeEpiphany

Thank you for your interest in contributing to CodeEpiphany! This document provides guidelines and information for contributors.

## Development Setup

### Prerequisites

- Java 17 or 21
- SBT (Scala Build Tool)
- Node.js 18+
- npm

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/WenjunHuang/CodeEpiphany.git
   cd CodeEpiphany
   ```

2. **Install webview dependencies**
   ```bash
   cd webview
   npm install
   cd ..
   ```

3. **Build the project**
   ```bash
   sbt compile
   ```

4. **Run tests**
   ```bash
   sbt test
   ```

### Quick Build Testing

To test the full build process locally (simulating CI):

- **Linux/macOS**: `./scripts/test-build.sh`
- **Windows**: `scripts\test-build.cmd`

These scripts will:
- Install npm dependencies
- Build the project with SBT
- Create the plugin artifact
- Verify the build output

## CI/CD Workflows

### Continuous Integration (CI)

The CI workflow runs on every push and pull request to `main` and `develop` branches:

- **Java Version**: Tests on Java 17
- **Dependencies**: Caches SBT and npm dependencies separately for better reliability
- **Webview Build**: Automatically built during SBT compilation
- **Plugin Build**: Creates plugin artifact using `packageArtifactZip`

### Code Quality

The code quality workflow ensures code standards:

- **Scala Formatting**: Uses Scalafmt for consistent code formatting
- **Scala Linting**: Uses Scalafix for code analysis
- **TypeScript Check**: Validates TypeScript compilation in webview

### Release

The release workflow triggers on version tags (`v*`):

- **Automated Releases**: Creates GitHub releases automatically
- **Artifacts**: Uploads built plugin ZIP files
- **Changelog**: Links to CHANGELOG.md for release notes

## Code Standards

### Scala Code

- Follow existing code style (enforced by Scalafmt)
- Run `sbt scalafmtAll` before committing
- Fix any Scalafix warnings: `sbt scalafixAll`

### TypeScript/JavaScript Code

- Use TypeScript for type safety
- Follow existing code patterns in webview directory
- Ensure `npm run build` passes without errors

### Commit Messages

Follow conventional commit format:

- `feat:` for new features
- `fix:` for bug fixes
- `chore:` for maintenance tasks
- `docs:` for documentation updates
- `refactor:` for code refactoring

Example: `feat: Add support for new coding platform`

## Pull Request Process

1. Create a feature branch from `develop`
2. Make your changes following the code standards
3. Test locally with the build scripts or manually
4. Format code with `sbt scalafmtAll`
5. Commit with conventional messages
6. Push and create PR to `develop` branch
7. Ensure CI passes - all checks must be green

## Release Process

1. Merge to main - Release PRs go from `develop` to `main`
2. Create tag - Push a version tag (e.g., `v1.2.3`)
3. Automated release - GitHub Actions will create the release

## Troubleshooting

### Common Issues

1. **Node.js cache errors**: The CI now uses separate npm cache actions to avoid path issues
2. **SBT build failures**: Use `packageArtifactZip` instead of deprecated `buildPlugin`
3. **Webview build**: The webview is automatically built during SBT compilation

### Getting Help

- **Issues**: [GitHub Issues](https://github.com/WenjunHuang/CodeEpiphany/issues)
- **Wiki**: Check the [project wiki](https://github.com/WenjunHuang/CodeEpiphany/wiki)

Thank you for contributing to CodeEpiphany! 🚀 