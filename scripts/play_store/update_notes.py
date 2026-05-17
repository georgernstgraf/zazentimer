
import os
import sys
import re
from google.oauth2 import service_account
from googleapiclient.discovery import build

def get_package_name():
    gradle_path = os.path.join(os.path.dirname(__file__), "../../app/build.gradle.kts")
    if not os.path.exists(gradle_path):
        return "at.priv.graf.zazentimer"
    with open(gradle_path, "r") as f:
        content = f.read()
        match = re.search(r'(namespace|applicationId)\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(2)
    return "at.priv.graf.zazentimer"

def update_release_notes(track_name, new_notes, language='de-DE'):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    key_path = os.path.join(script_dir, "../../google/play-api-key.json")
    package_name = get_package_name()

    if not os.path.exists(key_path):
        print(f"Error: Key not found at {key_path}")
        sys.exit(1)

    scopes = ['https://www.googleapis.com/auth/androidpublisher']
    credentials = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    service = build('androidpublisher', 'v3', credentials=credentials)

    # 1. Create a new edit
    edit_request = service.edits().insert(packageName=package_name, body={})
    edit = edit_request.execute()
    edit_id = edit['id']

    # 2. Get the track
    track = service.edits().tracks().get(
        packageName=package_name,
        editId=edit_id,
        track=track_name
    ).execute()

    if 'releases' not in track or not track['releases']:
        print(f"No releases found in track {track_name}")
        return

    # 3. Update notes in the latest release
    latest_release = track['releases'][0]
    if 'releaseNotes' not in latest_release:
        latest_release['releaseNotes'] = []
    
    found = False
    for note in latest_release['releaseNotes']:
        if note['language'] == language:
            note['text'] = new_notes
            found = True
            break
    
    if not found:
        latest_release['releaseNotes'].append({
            'language': language,
            'text': new_notes
        })

    # 4. Update the track in the edit
    service.edits().tracks().update(
        packageName=package_name,
        editId=edit_id,
        track=track_name,
        body=track
    ).execute()

    # 5. Commit the changes
    service.edits().commit(packageName=package_name, editId=edit_id).execute()
    print(f"Successfully updated {language} notes for track {track_name} ({package_name}).")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 update_notes.py <track> <notes> [language]")
        sys.exit(1)

    TRACK = sys.argv[1].lower()  # Force lowercase for Google Play API
    NOTES = sys.argv[2]
    LANG = sys.argv[3] if len(sys.argv) > 3 else 'de-DE'
    
    try:
        update_release_notes(TRACK, NOTES, LANG)
    except Exception as e:
        if "Track not found" in str(e):
            print(f"Error: Track '{TRACK}' not found. Use lowercase names like 'alpha', 'internal', 'beta', or 'production'.")
        else:
            print(f"Error: {e}")
        sys.exit(1)
