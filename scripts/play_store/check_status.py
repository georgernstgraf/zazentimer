
import os
import sys
import json
import re
from google.oauth2 import service_account
from googleapiclient.discovery import build

def get_package_name():
    """Extracts namespace/applicationId from build.gradle.kts."""
    gradle_path = os.path.join(os.path.dirname(__file__), "../../app/build.gradle.kts")
    if not os.path.exists(gradle_path):
        return "at.priv.graf.zazentimer"  # Fallback
    
    with open(gradle_path, "r") as f:
        content = f.read()
        # Look for namespace or applicationId
        match = re.search(r'(namespace|applicationId)\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(2)
    return "at.priv.graf.zazentimer"

def get_service(key_path):
    scopes = ['https://www.googleapis.com/auth/androidpublisher']
    credentials = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    return build('androidpublisher', 'v3', credentials=credentials)

def check_status():
    # Relative path from the script's location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    key_path = os.path.join(script_dir, "../../google/play-api-key.json")
    package_name = get_package_name()

    if not os.path.exists(key_path):
        print(f"Error: Key not found at {key_path}")
        sys.exit(1)

    print(f"Checking status for: {package_name}")
    service = get_service(key_path)
    
    # Check Tracks and Releases
    try:
        edit_request = service.edits().insert(packageName=package_name, body={})
        edit = edit_request.execute()
        edit_id = edit['id']
        
        tracks = service.edits().tracks().list(packageName=package_name, editId=edit_id).execute()
        
        print(f"\n--- Active Tracks for {package_name} ---")
        for track in tracks.get('tracks', []):
            name = track['track']
            print(f"\nTrack: {name.upper()}")
            for release in track.get('releases', []):
                ver = release.get('versionCodes', ['?'])
                status = release.get('status', 'unknown')
                release_name = release.get('name', 'no name')
                print(f"  [{status}] Version: {ver} ({release_name})")
                if 'releaseNotes' in release:
                    for note in release['releaseNotes']:
                        lang = note.get('language', '??')
                        text = note.get('text', 'No notes')
                        print(f"    ({lang}): {text}")
    except Exception as e:
        print(f"\nERROR: {e}")

if __name__ == "__main__":
    check_status()
