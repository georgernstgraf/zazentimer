
import os
import sys
from google.oauth2 import service_account
from googleapiclient.discovery import build

def update_release_notes(package_name, key_path, track_name, new_notes, language='de-DE'):
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
    
    # Ensure releaseNotes structure exists
    if 'releaseNotes' not in latest_release:
        latest_release['releaseNotes'] = []
    
    # Find existing note for language or add new one
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
    print(f"Successfully updated {language} notes for track {track_name}.")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 update_notes.py <track> <notes> [language]")
        sys.exit(1)

    TRACK = sys.argv[1]
    NOTES = sys.argv[2]
    LANG = sys.argv[3] if len(sys.argv) > 3 else 'de-DE'
    
    KEY_PATH = os.path.expanduser("~/.config/iron-country-322716-8ab0815de79f.json")
    PACKAGE_NAME = "at.priv.graf.zazentimer"
    
    try:
        update_release_notes(PACKAGE_NAME, KEY_PATH, TRACK, NOTES, LANG)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
