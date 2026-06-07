#!/bin/bash
set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Usage: $0 <version> (e.g. 3.0.8)"
    exit 1
fi

VERSION="${1#v}"

IFS='.' read -r major minor patch <<< "$VERSION"
VERSION_CODE=$((major * 1000000 + minor * 10000 + patch * 100))

echo "=== Preparing release v$VERSION (Code: $VERSION_CODE) ==="

cd "$(git rev-parse --show-toplevel)"

# Update .fdroid.yml version fields
sed -i -E "s/^(CurrentVersion: ).*/\1'$VERSION'/" .fdroid.yml
sed -i -E "s/^(CurrentVersionCode: ).*/\1$VERSION_CODE/" .fdroid.yml
sed -i -E "s/^(  - versionName: ).*/\1'$VERSION'/" .fdroid.yml
sed -i -E "s/^(    versionCode: ).*/\1$VERSION_CODE/" .fdroid.yml
sed -i -E "s/^(    commit: ).*/\1v$VERSION/" .fdroid.yml
sed -i -E "/^      - versionCode=/s/=.*/=$VERSION_CODE/" .fdroid.yml
sed -i -E "/^      - versionName=/s/=.*/=$VERSION/" .fdroid.yml

git add .fdroid.yml
git commit -m "chore: release v$VERSION"
git tag "v$VERSION"

echo ""
echo "=== Release v$VERSION ready ==="
echo "Commit: $(git rev-parse --short HEAD)"
echo ""
echo "To push: git push origin main v$VERSION"
