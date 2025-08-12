#!/usr/bin/env bash
set -euo pipefail

# === Config ===
BASE_URL="https://github.com/acsoto"
REPOS=(
  "Gem"
  "MedalCabinet"
  "GuildManager"
  "AtTackCraft-Core"
  "ItemManager"
  "AcShop"
  "GemShop"
)

# === Helpers ===

# Ensure working tree is clean
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "✖ Working tree has uncommitted changes. Commit/stash first."
  exit 1
fi

# Ensure current dir is a git repo
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
  echo "✖ Not inside a git repository."
  exit 1
}

# Auto initial commit if repo is empty (no commits yet)
if ! git rev-parse HEAD >/dev/null 2>&1; then
  echo "ℹ No commits in main repo. Creating initial commit."
  git commit --allow-empty -m "chore: initial commit"
fi

# Resolve default branch of a remote repo by reading remote HEAD
resolve_default_branch () {
  local remote_url="$1"
  # Example output: ref: refs/heads/main	HEAD
  local line
  if line=$(git ls-remote --symref "$remote_url" HEAD 2>/dev/null | head -n1); then
    # Extract branch name from 'refs/heads/<branch>'
    echo "$line" | awk '{print $2}' | awk -F'/' '{print $3}'
  else
    # Fallback if symref not available
    echo ""
  fi
}

# Sanitize remote name from repo name
remote_name_of () {
  # Lowercase, replace non-alnum with '-'
  echo "$1" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g'
}

# Add or update one repo using git subtree (preserve full history)
process_repo () {
  local repo="$1"
  local remote_name
  remote_name="$(remote_name_of "$repo")"
  local remote_url="${BASE_URL}/${repo}.git"

  # Add remote (or update URL if exists)
  if git remote get-url "$remote_name" >/dev/null 2>&1; then
    git remote set-url "$remote_name" "$remote_url"
  else
    git remote add "$remote_name" "$remote_url"
  fi

  # Detect default branch
  local def_branch
  def_branch="$(resolve_default_branch "$remote_url")"
  if [[ -z "$def_branch" ]]; then
    # Common fallbacks
    for b in main master trunk; do
      if git ls-remote --exit-code --heads "$remote_url" "$b" >/dev/null 2>&1; then
        def_branch="$b"; break
      fi
    done
  fi
  if [[ -z "$def_branch" ]]; then
    echo "✖ Cannot determine default branch for ${repo}. Skipping."
    return
  fi

  # Fetch only the default branch (faster)
  git fetch "$remote_name" "$def_branch"

  # Prefix dir equals repo name (customize if needed)
  local prefix_dir="modules/${repo}"

  if [[ -d "$prefix_dir" ]]; then
    # Already added before → pull updates into subtree
    echo "→ Updating subtree ${prefix_dir} from ${remote_name}/${def_branch}"
    git subtree pull --prefix="$prefix_dir" "$remote_name" "$def_branch" -m "chore(subtree): pull ${repo} (${def_branch})"
  else
    # First time add → preserve full history (NO --squash)
    echo "→ Adding subtree ${prefix_dir} from ${remote_name}/${def_branch}"
    git subtree add --prefix="$prefix_dir" "$remote_name" "$def_branch" -m "chore(subtree): add ${repo} (${def_branch})"
  fi

  # (Optional) keep remotes for future pulls; or uncomment next line to remove
  # git remote remove "$remote_name"
}

# === Run ===
for r in "${REPOS[@]}"; do
  process_repo "$r"
done

echo "✔ All done."

