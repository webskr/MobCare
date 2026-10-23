# Contributing to MobCare

Thank you for your interest in contributing to **MobCare**! We welcome bug reports, feature suggestions, security improvements, and pull requests.

MobCare is licensed under the **GNU General Public License v3.0 (GPLv3)**. By contributing, you agree that your contributions will be licensed under the same license terms.

---

## 🛠️ How Pull Requests Work

If you want to contribute code to this repository, here is the standard lifecycle:

```
[Your Fork of MobCare] ──> [Create Feature Branch] ──> [Commit & Push] ──> [Open Pull Request (PR)]
                                                                                     │
[Repository Owner (You)] <── [Automated Checks / Manual Review / Code Diff] <────────┘
         │
         ├──> [Request Changes / Discuss]
         └──> [Approve & Merge] ──> Code becomes part of official MobCare!
```

1. **Fork the Repository:** Click the **Fork** button on GitHub to create your personal copy of MobCare.
2. **Clone your Fork:**
   ```bash
   git clone https://github.com/your-username/MobCare.git
   cd MobCare
   ```
3. **Create a Topic Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
4. **Make Changes & Test:**
   - Ensure code compiles cleanly:
     ```bash
     ./gradlew compileDebugSources
     ```
   - Ensure clean code formatting following Kotlin Official Style Guidelines.
5. **Commit your Changes:**
   Write clear, concise commit messages:
   ```bash
   git commit -m "feat(app-locker): add biometric fallback option"
   ```
6. **Push to your Fork & Open a Pull Request (PR):**
   - Push to your branch on your fork:
     ```bash
     git push origin feature/your-feature-name
     ```
   - Go to the original MobCare GitHub repository. You will see a banner: **"Compare & pull request"**.
   - Fill in the description explaining what your PR changes, why it is needed, and any screenshots if UI is affected.
   - Click **Create Pull Request**.

---

## 🛡️ Code Guidelines & Standards

1. **Privacy First (Zero Tracker Policy):**
   - MobCare does not allow any third-party advertising SDKs, tracking libraries, or external analytics pings.
   - All telemetry and audit logs must stay on-device or sync only to owner-configured endpoints.

2. **Kotlin & Jetpack Best Practices:**
   - Use clean MVVM / Service architectures.
   - Background tasks must use Android Foreground Services with persistent notifications where required by Android 14+.
   - Resources (strings, colors, dimensions) must reside in `res/values/` and respect `button_colors.xml`.

3. **Security Standards:**
   - Never commit API keys, Google Cloud JSON files, or `.keystore` files.
   - Use SQLCipher / EncryptedSharedPreferences for sensitive storage.

---

## 📋 Pull Request Checklist

Before submitting your PR, verify:
- [ ] `./gradlew compileDebugSources` passes without errors.
- [ ] No secrets, keys, or unnecessary binaries are committed.
- [ ] Layout XMLs follow Material 3 dark charcoal styling (`#121316` background, `#1E1F24` cards).
- [ ] UI buttons adhere to `button_colors.xml` tokens.