# Frequently Asked Questions (FAQ) - GitHub Authentication & Synchronization

## Q1: Why does "GitHub Authorization Expired or Failed" occur during synchronization?

### Explanation
InMethod GitNoteTaking supports GitHub OAuth web login for authentication. According to GitHub's official security policy, if a GitHub OAuth App has enabled the "**Expire user authorization tokens**" setting, issued Access Tokens expire after **8 hours**.

When a token expires or is not refreshed past the 8-hour window, GitHub servers reject Git operations and respond with `HTTP 401 Unauthorized` (`not authorized`), preventing pushes and pulls.

---

## Q2: Will my notes be lost when authorization expires? How do I resume syncing?

**Your notes are completely safe and will not be lost!**

All locally edited notes are committed to your device's local Git repository. When synchronization encounters an expired token, the app initiates an automatic recovery and protection flow:

### Steps to Restore Sync:
1. **Tap Re-authenticate**: When the "GitHub Authorization Expired" dialog appears, tap "**Re-login & Sync**".
2. **Authorize via Browser**: The app opens your browser to the official GitHub authorization page. Tap "**Authorize**".
3. **Automatic Push Resume**: Upon successful re-authentication, the app returns to the foreground and **automatically retries and pushes the pending notes**, without requiring manual re-editing or manual sync clicks.

> **Tip**: You can also re-authenticate manually at any time by long-pressing the remote repository on the main screen -> selecting "**Modify**" -> tapping "**Re-authenticate with GitHub Web**".

---

## Q3: [Developers / OAuth App Admins] How to configure non-expiring GitHub tokens?

If you are the owner or administrator of the GitHub OAuth App (or running your own server), you can opt out of the 8-hour token expiration so authorizations never expire:

1. Log in to [GitHub](https://github.com/).
2. Click your profile avatar in the upper right corner -> go to **Settings**.
3. At the bottom of the left sidebar, click **Developer settings**.
4. Select **OAuth Apps**, and click on your GitNoteTaking application.
5. Scroll down to the **Optional features** section to find **Expire user authorization tokens**.
6. **Uncheck** this option (or choose to opt out).
7. Click **Save changes**.

> Once unchecked, all newly issued Access Tokens obtained by users signing into this OAuth App will be non-expiring (unless explicitly revoked by the user on GitHub), permanently resolving the 8-hour expiration issue.

---

## Q4: Can I use a GitHub Personal Access Token (PAT) instead?

**Yes!**

If you prefer not to use web OAuth, you can use a GitHub Personal Access Token:

1. Go to GitHub **Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)**.
2. Click **Generate new token (classic)**.
3. Set Note to `GitNoteTaking`, Expiration to **No expiration**, and check the **repo** scope.
4. Generate and copy the token (format: `ghp_xxxx...`).
5. In GitNoteTaking App:
   - For new repos: In the "Clone Remote Repository" screen, enter your GitHub username in Account, and paste the PAT into Password.
   - For existing repos: Long-press the repository in the list, select "Modify", paste the PAT into the Password field, and tap "OK".
